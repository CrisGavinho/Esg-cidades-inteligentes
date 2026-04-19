package com.esg.cidadesinteligentes.service;

import com.esg.cidadesinteligentes.model.IndicadorESG;
import com.esg.cidadesinteligentes.repository.IndicadorESGRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndicadorESGService {

    private final IndicadorESGRepository repository;

    public List<IndicadorESG> listarTodos() {
        return repository.findAll();
    }

    public Optional<IndicadorESG> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public List<IndicadorESG> buscarPorCidade(String cidade) {
        return repository.findByCidade(cidade);
    }

    public IndicadorESG salvar(IndicadorESG indicador) {
        return repository.save(indicador);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}
