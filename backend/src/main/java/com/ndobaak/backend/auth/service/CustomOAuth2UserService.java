package com.ndobaak.backend.auth.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerUserId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName() != null ? oidcUser.getGivenName() : "";
        String lastName = oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "";

        User user = userRepository.findByOauthProviderId(providerUserId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createOAuthStudent(email, firstName, lastName, providerUserId));

        if (user.getOauthProviderId() == null) {
            user.setOauthProvider("google");
            user.setOauthProviderId(providerUserId);
            userRepository.save(user);
        }

        return oidcUser;
    }

    private Student createOAuthStudent(String email, String firstName, String lastName, String providerUserId) {
        Student student = Student.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .oauthProvider("google")
                .oauthProviderId(providerUserId)
                .build();
        return (Student) userRepository.save(student);
    }
}
