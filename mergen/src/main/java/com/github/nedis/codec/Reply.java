package com.github.nedis.codec;

/**
 * User: roger
 * Date: 12-3-31 上午11:12
 */
public class Reply<T> {
    protected T value;
    protected String error;
    protected char marker;

    public Reply() {}
    public Reply(char marker, T value) {
        this.marker = marker;
        this.value = value;
    }
    public Reply(T value) {
        this.value = value;    
    }
    
    public T get() {
        return value;
    }
    
    public void set(T value) {
        this.value = value;
    }

}
