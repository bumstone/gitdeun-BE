package com.teamEWSN.gitdeun.common.converter;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Converter
@Component
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";
    private static String staticSecretKey;
    private Key key;

    @Value("${db.crypto-key}")
    public void setStaticSecretKey(String secretKey) {
        CryptoConverter.staticSecretKey = secretKey;
    }

    @PostConstruct
    public void init() {
        if (staticSecretKey == null || staticSecretKey.length() < 16) {
            throw new IllegalArgumentException("암호화 키는 16자 이상이어야 합니다.");
        }
        key = new SecretKeySpec(staticSecretKey.substring(0, 16).getBytes(), ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt attribute", e);
        }
    }
}

