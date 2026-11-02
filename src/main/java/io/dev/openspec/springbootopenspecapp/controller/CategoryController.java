package io.dev.openspec.springbootopenspecapp.controller;

import io.dev.openspec.springbootopenspecapp.common.dto.ProblemDetailSchema;
import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;
import io.dev.openspec.springbootopenspecapp.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.dev.openspec.springbootopenspecapp.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Categories", description = "Gerenciamento de categorias")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Cria uma categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "409", description = "Nome já existente", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Lista categorias ordenadas por nome")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorias"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @Operation(summary = "Consulta uma categoria por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "400", description = "Id inválido", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @Operation(summary = "Atualiza uma categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "409", description = "Nome já existente", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Operation(summary = "Remove uma categoria sem produtos")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria removida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class))),
            @ApiResponse(responseCode = "409", description = "Categoria possui produtos", content = @Content(mediaType = ProblemDetailSchema.MEDIA_TYPE, schema = @Schema(implementation = ProblemDetailSchema.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
