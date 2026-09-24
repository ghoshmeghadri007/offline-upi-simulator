package com.meghadri.offlineupi.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class ServerKeyHolder {
    private KeyPair keyPair;

    @PostConstruct
    public void init() throws Exception{
        KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        this.keyPair=gen.generateKeyPair();
    }

    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
