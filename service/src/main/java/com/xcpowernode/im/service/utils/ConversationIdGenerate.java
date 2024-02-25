package com.xcpowernode.im.service.utils;


public class ConversationIdGenerate {

    //A|B
    //B A
    public static String generateP2PId(String fromId,String toId){
        int i = fromId.compareTo(toId);
        if(i < 0){
            return toId+"|"+fromId;
        }else if(i > 0){
            return fromId+"|"+toId;
        }

        throw new RuntimeException("");
    }
}
