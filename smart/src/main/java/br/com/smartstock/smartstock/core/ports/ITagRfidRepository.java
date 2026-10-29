package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.TagRfid;

import java.util.List;
import java.util.Optional;

public interface ITagRfidRepository {
    Optional<TagRfid> findByCodigoRfid(String codigoRfid);
    Optional<TagRfid> findByPublicId(java.util.UUID publicId);
    List<TagRfid> findByProdutoId(Long produtoId);
    List<TagRfid> findAtivosByProdutoId(Long produtoId);
    List<TagRfid> findByLojaId(Long lojaId);
    Long save(TagRfid tag);
    void desvincular(String codigoRfid);
    void desvincularTodos(Long produtoId);
    boolean existsByCodigoRfid(String codigoRfid);
}
