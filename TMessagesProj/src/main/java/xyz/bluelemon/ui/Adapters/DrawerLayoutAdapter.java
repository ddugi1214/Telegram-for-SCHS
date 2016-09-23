/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package xyz.bluelemon.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import xyz.bluelemon.tfctalk.AndroidUtilities;
import xyz.bluelemon.tfctalk.LocaleController;
import xyz.bluelemon.tfctalk.MessagesController;
import xyz.bluelemon.tfctalk.UserConfig;
import xyz.bluelemon.ui.Cells.DrawerActionCell;
import xyz.bluelemon.ui.Cells.DividerCell;
import xyz.bluelemon.ui.Cells.EmptyCell;
import xyz.bluelemon.ui.Cells.DrawerProfileCell;

public class DrawerLayoutAdapter extends BaseAdapter {

    private Context mContext;

    public DrawerLayoutAdapter(Context context) {
        mContext = context;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return !(i == 0 || i == 1 || i == 5);
    }

    @Override
    public int getCount() {
        return UserConfig.isClientActivated() ? 10 : 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int type = getItemViewType(i);
        if (type == 0) {
            if (view == null) {
                view = new DrawerProfileCell(mContext);
            }
            ((DrawerProfileCell) view).setUser(MessagesController.getInstance().getUser(UserConfig.getClientUserId()));
        } else if (type == 1) {
            if (view == null) {
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
            }
        } else if (type == 2) {
            if (view == null) {
                view = new DividerCell(mContext);
            }
        } else if (type == 3) {
            if (view == null) {
                view = new DrawerActionCell(mContext);
            }
            DrawerActionCell actionCell = (DrawerActionCell) view;
            if (i == 2) {
                actionCell.setTextAndIcon(LocaleController.getString("NewGroup", xyz.bluelemon.tfctalk.R.string.NewGroup), xyz.bluelemon.tfctalk.R.drawable.menu_newgroup);
            } else if (i == 3) {
                actionCell.setTextAndIcon(LocaleController.getString("NewSecretChat", xyz.bluelemon.tfctalk.R.string.NewSecretChat), xyz.bluelemon.tfctalk.R.drawable.menu_secret);
            } else if (i == 4) {
                actionCell.setTextAndIcon(LocaleController.getString("NewChannel", xyz.bluelemon.tfctalk.R.string.NewChannel), xyz.bluelemon.tfctalk.R.drawable.menu_broadcast);
            } else if (i == 6) {
                actionCell.setTextAndIcon(LocaleController.getString("Contacts", xyz.bluelemon.tfctalk.R.string.Contacts), xyz.bluelemon.tfctalk.R.drawable.menu_contacts);
            } else if (i == 7) {
                actionCell.setTextAndIcon(LocaleController.getString("InviteFriends", xyz.bluelemon.tfctalk.R.string.InviteFriends), xyz.bluelemon.tfctalk.R.drawable.menu_invite);
            } else if (i == 8) {
                actionCell.setTextAndIcon(LocaleController.getString("Settings", xyz.bluelemon.tfctalk.R.string.Settings), xyz.bluelemon.tfctalk.R.drawable.menu_settings);
            } else if (i == 9) {
                actionCell.setTextAndIcon(LocaleController.getString("TelegramFaq", xyz.bluelemon.tfctalk.R.string.TelegramFaq), xyz.bluelemon.tfctalk.R.drawable.menu_help);
            }
        }

        return view;
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        } else if (i == 5) {
            return 2;
        }
        return 3;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return !UserConfig.isClientActivated();
    }
}
