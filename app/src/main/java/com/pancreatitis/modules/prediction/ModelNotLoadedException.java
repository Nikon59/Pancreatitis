package com.pancreatitis.modules.prediction;

public class ModelNotLoadedException extends RuntimeException {
    public ModelNotLoadedException(String message) {
        super(message);
    }

    public ModelNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }
}
