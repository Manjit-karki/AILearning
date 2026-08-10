package com.example.Backend.test;

import com.example.Backend.model.SUser;
import com.example.Backend.repository.UserRepository;
import com.example.Backend.services.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private SUser mockUser;
    private final String username = "johndoe";

    @BeforeEach
    void setUp() {
        mockUser = new SUser();
        mockUser.setUserId("user123");
        mockUser.setUsername(username);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("encodedOldPassword");
    }

    @Nested
    @DisplayName("getProfile Tests")
    class GetProfileTests {

        @Test
        @DisplayName("getProfile - Should return SUser entity when user exists")
        void getProfile_WhenUserExists_ShouldReturnUser() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));

            SUser response = profileService.getProfile(username);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(mockUser.getUserId());
            assertThat(response.getUsername()).isEqualTo(mockUser.getUsername());
            assertThat(response.getName()).isEqualTo(mockUser.getName());
            assertThat(response.getEmail()).isEqualTo(mockUser.getEmail());
        }

        @Test
        @DisplayName("getProfile - Should throw exception when user not found")
        void getProfile_WhenUserNotFound_ShouldThrowException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(username))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateProfile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("updateProfile - Should throw exception when user not found")
        void updateProfile_WhenUserNotFound_ShouldThrowException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(username, "New Name", "new@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("updateProfile - Should update name and email when inputs are valid and email is unique")
        void updateProfile_WhenInputsAreValid_ShouldUpdateAndReturn() {
            String newName = "Jane Doe";
            String newEmail = "jane@example.com";

            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.existsByEmail(newEmail)).thenReturn(false);
            when(userRepository.save(any(SUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SUser response = profileService.updateProfile(username, newName, newEmail);

            assertThat(response.getName()).isEqualTo(newName);
            assertThat(response.getEmail()).isEqualTo(newEmail);
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("updateProfile - Should throw exception when new email is already in use")
        void updateProfile_WhenEmailAlreadyInUse_ShouldThrowException() {
            String newEmail = "taken@example.com";

            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.existsByEmail(newEmail)).thenReturn(true);

            assertThatThrownBy(() -> profileService.updateProfile(username, "Jane Doe", newEmail))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email already in use");

            verify(userRepository, never()).save(any(SUser.class));
        }

        @Test
        @DisplayName("updateProfile - Should not check email existence if new email matches current email")
        void updateProfile_WhenEmailUnchanged_ShouldNotCheckExistence() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(SUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SUser response = profileService.updateProfile(username, "New Name", mockUser.getEmail());

            assertThat(response.getName()).isEqualTo("New Name");
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("updateProfile - Should ignore null or blank values without updating fields")
        void updateProfile_WhenInputsAreNullOrBlank_ShouldKeepOriginalValues() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(SUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SUser response = profileService.updateProfile(username, "    ", null);

            assertThat(response.getName()).isEqualTo("John Doe");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            verify(userRepository).save(mockUser);
        }
    }

    @Nested
    @DisplayName("changePassword Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("changePassword - Should throw exception when user not found")
        void changePassword_WhenUserNotFound_ShouldThrowException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.changePassword(username, "oldPass", "newSecretPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("changePassword - Should throw exception when current password does not match")
        void changePassword_WhenCurrentPasswordIncorrect_ShouldThrowException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrongPass", mockUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> profileService.changePassword(username, "wrongPass", "newSecretPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Current password is incorrect");

            verify(userRepository, never()).save(any(SUser.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345", "", "a"})
        @DisplayName("changePassword - Should throw exception when new password is too short")
        void changePassword_WhenNewPasswordShort_ShouldThrowException(String shortPassword) {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("correctPass", mockUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> profileService.changePassword(username, "correctPass", shortPassword))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("New password must be at least 6 characters long");

            verify(userRepository, never()).save(any(SUser.class));
        }

        @Test
        @DisplayName("changePassword - Should throw exception when new password is null")
        void changePassword_WhenNewPasswordNull_ShouldThrowException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("correctPass", mockUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> profileService.changePassword(username, "correctPass", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("New password must be at least 6 characters long");

            verify(userRepository, never()).save(any(SUser.class));
        }

        @Test
        @DisplayName("changePassword - Should encode new password and save user when inputs are valid")
        void changePassword_WhenValidInputs_ShouldEncodeAndSave() {
            String rawOldPass = "correctOldPass";
            String rawNewPass = "newValidPassword123";
            String encodedNewPass = "encodedNewPassword123";

            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(rawOldPass, mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(rawNewPass)).thenReturn(encodedNewPass);

            profileService.changePassword(username, rawOldPass, rawNewPass);

            assertThat(mockUser.getPassword()).isEqualTo(encodedNewPass);
            verify(passwordEncoder).encode(rawNewPass);
            verify(userRepository).save(mockUser);
        }
    }

    @Nested
    @DisplayName("Additional Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("updateProfile - Should update ONLY name when newEmail is null")
        void updateProfile_WhenOnlyNameProvided_ShouldUpdateNameOnly() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(SUser.class))).thenAnswer(inv -> inv.getArgument(0));

            SUser response = profileService.updateProfile(username, "New Name Only", null);

            assertThat(response.getName()).isEqualTo("New Name Only");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("updateProfile - Should update ONLY email when newName is blank")
        void updateProfile_WhenOnlyEmailProvided_ShouldUpdateEmailOnly() {
            String newEmail = "newonly@example.com";
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.existsByEmail(newEmail)).thenReturn(false);
            when(userRepository.save(any(SUser.class))).thenAnswer(inv -> inv.getArgument(0));

            SUser response = profileService.updateProfile(username, "    ", newEmail);

            assertThat(response.getName()).isEqualTo("John Doe");
            assertThat(response.getEmail()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("changePassword - Should succeed when new password is exactly 6 characters")
        void changePassword_WhenPasswordLengthExactlySix_ShouldSucceed() {
            String pass6Chars = "123456";
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("oldPass", mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(pass6Chars)).thenReturn("encoded123456");

            profileService.changePassword(username, "oldPass", pass6Chars);

            verify(userRepository).save(mockUser);
            assertThat(mockUser.getPassword()).isEqualTo("encoded123456");
        }

        @Test
        @DisplayName("updateProfile - Should propagate exception if database save fails")
        void updateProfile_WhenSaveFails_ShouldPropagateException() {
            when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(SUser.class))).thenThrow(new RuntimeException("Database write failed"));

            assertThatThrownBy(() -> profileService.updateProfile(username, "New Name", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database write failed");
        }
    }
}