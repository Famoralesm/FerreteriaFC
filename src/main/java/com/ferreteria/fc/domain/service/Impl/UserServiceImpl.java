package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.UserMapper;
import com.ferreteria.fc.domain.dto.request.UserRequest;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.domain.enums.ImageKind;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.UserRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import com.ferreteria.fc.domain.dto.request.UpdateMyProfileRequest;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.ImageStorageService.StoredImage;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;
    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ImageStorageService imageStorageService,
            AuditLogService auditLogService,
            StoreRepository storeRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageStorageService = imageStorageService;
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
    }

    @Override
    public UserResponse create(UserRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con ese correo"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria"
            );
        }

        Store sucursal = resolveStore(request.getStoreId());
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(request.getActive() == null ||
                        request.getActive())
                .sucursal(sucursal)
                .build();

        User saved = userRepository.save(user);
        auditLogService.recordForCurrentUser(
                "USERS", "USER", saved.getId().toString(), "CREATED", null,
                null, Map.of("name", saved.getName(), "role", saved.getRole().name(),
                        "active", saved.isActive(), "sucursal", sucursal.getNombre(),
                        "changedFields", List.of("name", "email", "role", "active", "sucursal")),
                "Usuario creado", "WEB"
        );
        return UserMapper.toResponse(saved);
    }

    @Override
    public List<UserResponse> getAll() {

        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse getById(Long id) {

        return UserMapper.toResponse(findUser(id));
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {

        User user = findUser(id);
        Role previousRole = user.getRole();
        boolean previousActive = user.isActive();
        String previousName = user.getName();
        String previousEmail = user.getEmail();
        Long previousStoreId = user.getSucursal() != null ? user.getSucursal().getId() : null;
        String previousStoreName = user.getSucursal() != null ? user.getSucursal().getNombre() : null;

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException(
                    "El correo ya está siendo utilizado por otro usuario"
            );
        }

        // No permitimos desactivar al último administrador activo.
        Role newRole = request.getRole() != null
                ? request.getRole()
                : user.getRole();

        boolean newActive = request.getActive() != null
                ? request.getActive()
                : user.isActive();

        boolean isRemovingActiveAdmin =
                user.getRole() == Role.ADMIN &&
                        user.isActive() &&
                        (newRole != Role.ADMIN || !newActive);

        if (isRemovingActiveAdmin) {

            long administrators =
                    userRepository.countByRoleAndActiveTrue(Role.ADMIN);

            if (administrators <= 1) {
                throw new IllegalStateException(
                        "No puedes desactivar o cambiar el perfil del último administrador activo"
                );
            }
        }




        Store sucursal = resolveStore(request.getStoreId());
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setRole(newRole);
        user.setActive(newActive);
        user.setSucursal(sucursal);


        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        User saved = userRepository.save(user);
        String action = previousActive && !saved.isActive() ? "DEACTIVATED"
                : !previousActive && saved.isActive() ? "ACTIVATED" : "UPDATED";
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(previousName, saved.getName())) changedFields.add("name");
        if (!Objects.equals(previousEmail, saved.getEmail())) changedFields.add("email");
        if (previousRole != saved.getRole()) changedFields.add("role");
        if (previousActive != saved.isActive()) changedFields.add("active");
        if (!Objects.equals(previousStoreId, saved.getSucursal() != null ? saved.getSucursal().getId() : null)) changedFields.add("sucursal");
        if (request.getPassword() != null && !request.getPassword().isBlank()) changedFields.add("password");
        if (!changedFields.isEmpty()) {
            Map<String, Object> oldValues = new LinkedHashMap<>();
            oldValues.put("name", previousName);
            oldValues.put("role", previousRole.name());
            oldValues.put("active", previousActive);
            oldValues.put("sucursal", previousStoreName);
            oldValues.put("changedFields", changedFields);

            Map<String, Object> newValues = new LinkedHashMap<>();
            newValues.put("name", saved.getName());
            newValues.put("role", saved.getRole().name());
            newValues.put("active", saved.isActive());
            newValues.put("sucursal", saved.getSucursal() != null ? saved.getSucursal().getNombre() : null);
            newValues.put("changedFields", changedFields);

            auditLogService.recordForCurrentUser(
                    "USERS", "USER", saved.getId().toString(), action, null,
                    oldValues, newValues, "Usuario actualizado", "WEB"
            );
        }
        return UserMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {

        User user = findUser(id);

        if (user.getRole() == Role.ADMIN && user.isActive()) {

            long administrators =
                    userRepository.countByRoleAndActiveTrue(Role.ADMIN);

            if (administrators <= 1) {
                throw new IllegalStateException(
                        "No puedes eliminar al último administrador activo"
                );
            }
        }

        auditLogService.recordForCurrentUser(
                "USERS", "USER", user.getId().toString(), "DELETED", null,
                Map.of("role", user.getRole().name(), "active", user.isActive()),
                null, "Usuario eliminado", "WEB"
        );
        userRepository.delete(user);
    }

    private Store resolveStore(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("Debe seleccionar una sucursal");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        if (!Boolean.TRUE.equals(store.getActiva())) {
            throw new IllegalArgumentException("No se puede asignar un usuario a una sucursal inactiva");
        }
        return store;
    }

    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado: " + id
                        )
                );
    }

    public User getByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public Optional<User> getOptionalByEmail(String nombreUsuario){
        return userRepository.findOptionalByEmail(nombreUsuario);
    }

    @Override
    public UserResponse updateMyProfile(
            String email,
            UpdateMyProfileRequest request
    ) {

        User user = requireUser(email);

        String previousName = user.getName();
        user.setName(request.getName().trim());
        User saved = userRepository.save(user);
        if (!java.util.Objects.equals(previousName, saved.getName())) {
            auditLogService.recordForCurrentUser(
                    "USERS", "USER", saved.getId().toString(), "UPDATED", null,
                    Map.of("profileNameChanged", false),
                    Map.of("profileNameChanged", true),
                    "Perfil de usuario actualizado", "WEB"
            );
        }
        return UserMapper.toResponse(saved);

    }

    @Override
    public UserResponse uploadMyAvatar(
            String email,
            MultipartFile file
    ) {

        User user = requireUser(email);

        String previousPath = user.getAvatarPath();

        StoredImage image = imageStorageService.store(
                file,
                ImageKind.USER_AVATAR
        );

        user.setAvatarPath(image.relativePath());
        user.setAvatarSha256(image.sha256());
        user.setAvatarSizeBytes(image.sizeBytes());
        user.setAvatarWidth(image.width());
        user.setAvatarHeight(image.height());

        User saved = userRepository.saveAndFlush(user);

        deleteUnusedAvatar(previousPath, saved.getAvatarPath());

        return UserMapper.toResponse(saved);

    }

    @Override
    public UserResponse deleteMyAvatar(String email) {

        User user = requireUser(email);

        String previousPath = user.getAvatarPath();

        user.setAvatarPath(null);
        user.setAvatarSha256(null);
        user.setAvatarSizeBytes(null);
        user.setAvatarWidth(null);
        user.setAvatarHeight(null);

        User saved = userRepository.saveAndFlush(user);

        deleteUnusedAvatar(previousPath, null);

        return UserMapper.toResponse(saved);

    }

    private User requireUser(String email) {

        return userRepository.findOptionalByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado"
                        )
                );

    }

    private void deleteUnusedAvatar(
            String previousPath,
            String currentPath
    ) {

        if (
                previousPath != null &&
                        !previousPath.equals(currentPath) &&
                        userRepository.countByAvatarPath(previousPath) == 0
        ) {

            imageStorageService.delete(previousPath);

        }

    }

    @Override
    public UserResponse uploadUserAvatar(Long id, MultipartFile file) {

        User user = findUser(id);

        String previousPath = user.getAvatarPath();

        StoredImage image = imageStorageService.store(
                file,
                ImageKind.USER_AVATAR
        );

        user.setAvatarPath(image.relativePath());
        user.setAvatarSha256(image.sha256());
        user.setAvatarSizeBytes(image.sizeBytes());
        user.setAvatarWidth(image.width());
        user.setAvatarHeight(image.height());

        User saved = userRepository.saveAndFlush(user);

        deleteUnusedAvatar(previousPath, saved.getAvatarPath());

        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse deleteUserAvatar(Long id) {

        User user = findUser(id);

        String previousPath = user.getAvatarPath();

        user.setAvatarPath(null);
        user.setAvatarSha256(null);
        user.setAvatarSizeBytes(null);
        user.setAvatarWidth(null);
        user.setAvatarHeight(null);

        User saved = userRepository.saveAndFlush(user);

        deleteUnusedAvatar(previousPath, null);

        return UserMapper.toResponse(saved);
    }
}