package com.thuantan2060.ppio.Message;

/**
 * Created by choxu on 014/14/3/2016.
 */
public class EmitMessage {
    public String tag;
    public Object data;

    public EmitMessage(String tag, Object data) {
        this.tag = tag;
        this.data = data;
    }
}
