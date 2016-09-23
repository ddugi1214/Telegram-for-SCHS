package xyz.bluelemon.tgnet;

public interface RequestDelegateInternal {
    void run(int response, int errorCode, String errorText);
}
