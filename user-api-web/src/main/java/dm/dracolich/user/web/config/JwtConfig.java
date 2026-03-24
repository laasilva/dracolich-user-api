package dm.dracolich.user.web.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    private Resource privateKey;
    private Resource publicKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
    private long confirmationTokenExpiration;

    private ECPrivateKey ecPrivateKey;
    private ECPublicKey ecPublicKey;

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        byte[] privateKeyBytes = parsePem(privateKey);
        ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        byte[] publicKeyBytes = parsePem(publicKey);
        ecPublicKey = (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    private byte[] parsePem(Resource resource) throws IOException {
        String pem = new String(resource.getContentAsByteArray());
        String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
