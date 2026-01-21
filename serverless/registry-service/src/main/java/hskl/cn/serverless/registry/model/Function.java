package hskl.cn.serverless.registry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "functions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Function {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String runtime;

    @Column(nullable = false)
    private String handler;

    @Column(name = "jar_path")
    private String jarPath;

    @Column(name = "jar_size")
    private Long jarSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FunctionStatus status = FunctionStatus.PENDING;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Column(name = "memory_mb")
    @Builder.Default
    private Integer memoryMb = 256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FunctionStatus {
        PENDING,
        READY,
        DISABLED,
        ERROR
    }
}
