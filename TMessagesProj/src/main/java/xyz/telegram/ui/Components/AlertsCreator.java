/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package xyz.telegram.ui.Components;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import xyz.telegram.messenger.LocaleController;
import xyz.telegram.messenger.MessagesController;
import xyz.telegram.messenger.MessagesStorage;
import xyz.telegram.messenger.NotificationsController;
import xyz.telegram.messenger.ApplicationLoader;
import xyz.telegram.messenger.Utilities;
import xyz.telegram.tgnet.ConnectionsManager;
import xyz.telegram.tgnet.RequestDelegate;
import xyz.telegram.tgnet.TLObject;
import xyz.telegram.tgnet.TLRPC;
import xyz.telegram.ui.ActionBar.BaseFragment;
import xyz.telegram.ui.ActionBar.BottomSheet;
import xyz.telegram.ui.ReportOtherActivity;

public class AlertsCreator {

    public static Dialog createMuteAlert(Context context, final long dialog_id) {
        if (context == null) {
            return null;
        }

        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        builder.setTitle(LocaleController.getString("Notifications", xyz.telegram.messenger.R.string.Notifications));
        CharSequence[] items = new CharSequence[]{
                LocaleController.formatString("MuteFor", xyz.telegram.messenger.R.string.MuteFor, LocaleController.formatPluralString("Hours", 1)),
                LocaleController.formatString("MuteFor", xyz.telegram.messenger.R.string.MuteFor, LocaleController.formatPluralString("Hours", 8)),
                LocaleController.formatString("MuteFor", xyz.telegram.messenger.R.string.MuteFor, LocaleController.formatPluralString("Days", 2)),
                LocaleController.getString("MuteDisable", xyz.telegram.messenger.R.string.MuteDisable)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int untilTime = ConnectionsManager.getInstance().getCurrentTime();
                        if (i == 0) {
                            untilTime += 60 * 60;
                        } else if (i == 1) {
                            untilTime += 60 * 60 * 8;
                        } else if (i == 2) {
                            untilTime += 60 * 60 * 48;
                        } else if (i == 3) {
                            untilTime = Integer.MAX_VALUE;
                        }

                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        long flags;
                        if (i == 3) {
                            editor.putInt("notify2_" + dialog_id, 2);
                            flags = 1;
                        } else {
                            editor.putInt("notify2_" + dialog_id, 3);
                            editor.putInt("notifyuntil_" + dialog_id, untilTime);
                            flags = ((long) untilTime << 32) | 1;
                        }
                        NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
                        MessagesStorage.getInstance().setDialogFlags(dialog_id, flags);
                        editor.commit();
                        TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                        if (dialog != null) {
                            dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                            dialog.notify_settings.mute_until = untilTime;
                        }
                        NotificationsController.updateServerNotificationsSettings(dialog_id);
                    }
                }
        );
        return builder.create();
    }

    public static Dialog createReportAlert(Context context, final long dialog_id, final BaseFragment parentFragment) {
        if (context == null || parentFragment == null) {
            return null;
        }

        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        builder.setTitle(LocaleController.getString("ReportChat", xyz.telegram.messenger.R.string.ReportChat));
        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("ReportChatSpam", xyz.telegram.messenger.R.string.ReportChatSpam),
                LocaleController.getString("ReportChatViolence", xyz.telegram.messenger.R.string.ReportChatViolence),
                LocaleController.getString("ReportChatPornography", xyz.telegram.messenger.R.string.ReportChatPornography),
                LocaleController.getString("ReportChatOther", xyz.telegram.messenger.R.string.ReportChatOther)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 3) {
                            Bundle args = new Bundle();
                            args.putLong("dialog_id", dialog_id);
                            parentFragment.presentFragment(new ReportOtherActivity(args));
                            return;
                        }
                        TLRPC.TL_account_reportPeer req = new TLRPC.TL_account_reportPeer();
                        req.peer = MessagesController.getInputPeer((int) dialog_id);
                        if (i == 0) {
                            req.reason = new TLRPC.TL_inputReportReasonSpam();
                        } else if (i == 1) {
                            req.reason = new TLRPC.TL_inputReportReasonViolence();
                        } else if (i == 2) {
                            req.reason = new TLRPC.TL_inputReportReasonPornography();
                        }
                        ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                            @Override
                            public void run(TLObject response, TLRPC.TL_error error) {

                            }
                        });
                    }
                }
        );
        return builder.create();
    }

    public static void showFloodWaitAlert(String error, final BaseFragment fragment) {
        if (error == null || !error.startsWith("FLOOD_WAIT") || fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        int time = Utilities.parseInt(error);
        String timeString;
        if (time < 60) {
            timeString = LocaleController.formatPluralString("Seconds", time);
        } else {
            timeString = LocaleController.formatPluralString("Minutes", time / 60);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", xyz.telegram.messenger.R.string.AppName));
        builder.setMessage(LocaleController.formatString("FloodWaitTime", xyz.telegram.messenger.R.string.FloodWaitTime, timeString));
        builder.setPositiveButton(LocaleController.getString("OK", xyz.telegram.messenger.R.string.OK), null);
        fragment.showDialog(builder.create(), true);
    }

    public static void showAddUserAlert(String error, final BaseFragment fragment, boolean isChannel) {
        if (error == null || fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", xyz.telegram.messenger.R.string.AppName));
        switch (error) {
            case "PEER_FLOOD":
                builder.setMessage(LocaleController.getString("NobodyLikesSpam2", xyz.telegram.messenger.R.string.NobodyLikesSpam2));
                builder.setNegativeButton(LocaleController.getString("MoreInfo", xyz.telegram.messenger.R.string.MoreInfo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MessagesController.openByUserName("spambot", fragment, 1);
                    }
                });
                break;
            case "USER_BLOCKED":
            case "USER_BOT":
            case "USER_ID_INVALID":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("ChannelUserCantAdd", xyz.telegram.messenger.R.string.ChannelUserCantAdd));
                } else {
                    builder.setMessage(LocaleController.getString("GroupUserCantAdd", xyz.telegram.messenger.R.string.GroupUserCantAdd));
                }
                break;
            case "USERS_TOO_MUCH":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("ChannelUserAddLimit", xyz.telegram.messenger.R.string.ChannelUserAddLimit));
                } else {
                    builder.setMessage(LocaleController.getString("GroupUserAddLimit", xyz.telegram.messenger.R.string.GroupUserAddLimit));
                }
                break;
            case "USER_NOT_MUTUAL_CONTACT":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("ChannelUserLeftError", xyz.telegram.messenger.R.string.ChannelUserLeftError));
                } else {
                    builder.setMessage(LocaleController.getString("GroupUserLeftError", xyz.telegram.messenger.R.string.GroupUserLeftError));
                }
                break;
            case "ADMINS_TOO_MUCH":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("ChannelUserCantAdmin", xyz.telegram.messenger.R.string.ChannelUserCantAdmin));
                } else {
                    builder.setMessage(LocaleController.getString("GroupUserCantAdmin", xyz.telegram.messenger.R.string.GroupUserCantAdmin));
                }
                break;
            case "BOTS_TOO_MUCH":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("ChannelUserCantBot", xyz.telegram.messenger.R.string.ChannelUserCantBot));
                } else {
                    builder.setMessage(LocaleController.getString("GroupUserCantBot", xyz.telegram.messenger.R.string.GroupUserCantBot));
                }
                break;
            case "USER_PRIVACY_RESTRICTED":
                if (isChannel) {
                    builder.setMessage(LocaleController.getString("InviteToChannelError", xyz.telegram.messenger.R.string.InviteToChannelError));
                } else {
                    builder.setMessage(LocaleController.getString("InviteToGroupError", xyz.telegram.messenger.R.string.InviteToGroupError));
                }
                break;
            case "USERS_TOO_FEW":
                builder.setMessage(LocaleController.getString("CreateGroupError", xyz.telegram.messenger.R.string.CreateGroupError));
                break;
            case "USER_RESTRICTED":
                builder.setMessage(LocaleController.getString("UserRestricted", xyz.telegram.messenger.R.string.UserRestricted));
                break;
            default:
                builder.setMessage(error);
                break;
        }
        builder.setPositiveButton(LocaleController.getString("OK", xyz.telegram.messenger.R.string.OK), null);
        fragment.showDialog(builder.create(), true);
    }
}
