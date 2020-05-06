package net.corda.samples.client;

/**
 * A wrapper to send response from the rest calls.
 * @param <T>
 */
public class APIResponse<T> {
    private String message;
    private T data;
    private boolean status;

    public APIResponse(String message, T data, boolean status) {
        this.message = message;
        this.data = data;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isStatus() {
        return status;
    }

    public static <T> APIResponse<T> success(){
        return new APIResponse<>("SUCCESS", null, true);
    }

    public static <T> APIResponse<T> success(T data){
        return new APIResponse<>("SUCCESS", data, true);
    }

    public static <T> APIResponse<T> error(String message){
        return new APIResponse<>(message, null, false);
    }
}
