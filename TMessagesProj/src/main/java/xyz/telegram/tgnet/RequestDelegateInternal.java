package xyz.telegram.tgnet;

public interface RequestDelegateInternal {
    void run(int response, int errorCode, String errorText);
}
