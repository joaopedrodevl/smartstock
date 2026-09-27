package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAdminRepository {
    public void save(Admin admin);
    public Optional<Admin> findByEmail(String email);
    public Optional<Admin> findById(Long Id);
    public Optional<Admin> findByPublicId(UUID publicId);
    public Optional<Admin> findByLojaIdAndRole(Long lojaId, String role);
    public List<Admin> findAll();
    public void update(Admin admin);
    public void updateSenha(Long id, String senhaHash);
    public void atualizarUltimoLogin(Long id);
    public void delete(Long id);
}
