package com.esg.cidadesinteligentes.controller;

import com.esg.cidadesinteligentes.model.IndicadorESG;
import com.esg.cidadesinteligentes.service.IndicadorESGService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadorESGController {

    private final IndicadorESGService service;

    @GetMapping
    public List<IndicadorESG> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IndicadorESG> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cidade/{cidade}")
    public List<IndicadorESG> buscarPorCidade(@PathVariable String cidade) {
        return service.buscarPorCidade(cidade);
    }

    @PostMapping
    public ResponseEntity<IndicadorESG> criar(@Valid @RequestBody IndicadorESG indicador) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(indicador));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IndicadorESG> atualizar(@PathVariable Long id, @Valid @RequestBody IndicadorESG indicador) {
        return service.buscarPorId(id).map(existing -> {
            indicador.setId(id);
            return ResponseEntity.ok(service.salvar(indicador));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
