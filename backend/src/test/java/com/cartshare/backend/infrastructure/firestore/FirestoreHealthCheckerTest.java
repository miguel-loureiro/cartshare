package com.cartshare.backend.infrastructure.firestore;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import java.util.Iterator;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirestoreHealthCheckerTest {

    @Mock
    private Firestore firestore;

    @Mock
    private FirestoreOptions firestoreOptions;

    @Mock
    private ApplicationArguments args;

    private FirestoreHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        healthChecker = new FirestoreHealthChecker(firestore);
    }

    @Test
    void run_ShouldLogSuccess_WhenFirestoreIsAccessible() {
        // Arrange: Simular a cadeia firestore.listCollections().iterator().hasNext()
        Iterable<CollectionReference> mockIterable = mock(Iterable.class);
        Iterator<CollectionReference> mockIterator = mock(Iterator.class);

        when(firestore.listCollections()).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true);

        // Simular firestore.getOptions().getProjectId()
        when(firestore.getOptions()).thenReturn(firestoreOptions);
        when(firestoreOptions.getProjectId()).thenReturn("test-project-id");

        // Act
        healthChecker.run(args);

        // Assert: Verificar se o método principal foi chamado
        verify(firestore, times(1)).listCollections();
        verify(firestore, times(1)).getOptions();
    }

    @Test
    void run_ShouldLogFailure_WhenFirestoreThrowsException() {
        // Arrange: Forçar uma exceção ao tentar listar coleções
        when(firestore.listCollections()).thenThrow(new RuntimeException("Connection Timeout"));

        // Act
        // O método captura a exceção internamente, então não precisamos de assertThrows
        healthChecker.run(args);

        // Assert: Verificar que a tentativa foi feita e o catch foi acionado
        verify(firestore, times(1)).listCollections();
        // O getOptions() não deve ser chamado se houver erro antes
        verify(firestore, never()).getOptions();
    }
}