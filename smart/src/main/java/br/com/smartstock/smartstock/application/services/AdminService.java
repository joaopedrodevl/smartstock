package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.entities.Loja;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.core.valueobjects.Email;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
import br.com.smartstock.smartstock.infra.api.dtos.AdminRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.VendaAdminRequestDTO;
import br.com.smartstock.smartstock.core.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final IAdminRepository adminRepository;
    private final ILojaRepository lojaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(IAdminRepository adminRepository, ILojaRepository lojaRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.lojaRepository = lojaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin criar(AdminRequestDTO dto) {
        log.info("Criando administrador - email: {}", dto.email());

        if (adminRepository.findByEmail(dto.email()).isPresent()) {
            log.warn("Tentativa de criar administrador com email já cadastrado: {}", dto.email());
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Admin admin = Admin.criar(
                Nome.of(dto.nome()),
                Telefone.of(dto.telefone()),
                Email.of(dto.email()),
                passwordEncoder.encode(dto.senha())
        );

        adminRepository.save(admin);
        log.info("Administrador criado com sucesso - email: {}", dto.email());
        return admin;
    }

    public Admin criarVendaAdmin(VendaAdminRequestDTO dto) {
        log.info("Criando vendedor (ROLE_VENDA_ADMIN) - email: {}, lojaPublicId: {}", dto.email(), dto.lojaPublicId());

        Loja loja = lojaRepository.findByPublicId(dto.lojaPublicId())
                .orElseThrow(() -> new IllegalArgumentException("Loja não encontrada"));

        if (adminRepository.findByLojaIdAndRole(loja.getId(), Role.ROLE_VENDA_ADMIN.name()).isPresent()) {
            log.warn("Já existe um vendedor para a loja - lojaPublicId: {}", dto.lojaPublicId());
            throw new IllegalArgumentException("Já existe um vendedor para esta loja");
        }

        if (adminRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Admin admin = Admin.criarVendaAdmin(
                Nome.of(dto.nome()),
                Telefone.of(dto.telefone()),
                Email.of(dto.email()),
                passwordEncoder.encode(dto.senha()),
                loja.getId()
        );

        adminRepository.save(admin);
        log.info("Vendedor criado com sucesso - email: {}, lojaId: {}", dto.email(), loja.getId());
        return admin;
    }

    public Optional<Admin> buscarVendaAdminPorLoja(UUID lojaPublicId) {
        Loja loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Loja não encontrada"));
        return adminRepository.findByLojaIdAndRole(loja.getId(), Role.ROLE_VENDA_ADMIN.name());
    }
}
