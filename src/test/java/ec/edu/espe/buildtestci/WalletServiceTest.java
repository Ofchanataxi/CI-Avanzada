package ec.edu.espe.buildtestci;

import ec.edu.espe.buildtestci.dto.WalletResponse;
import ec.edu.espe.buildtestci.model.Wallet;
import ec.edu.espe.buildtestci.repository.WalletRepository;
import ec.edu.espe.buildtestci.service.RiskClient;
import ec.edu.espe.buildtestci.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletService walletService;
    private RiskClient riskClient;

    @BeforeEach
    public void setUp() {
        walletRepository = Mockito.mock(WalletRepository.class);
        riskClient = Mockito.mock(RiskClient.class);
        walletService = new WalletService(walletRepository, riskClient);
    }

    @Test
    void createWallet_validData_shouldSaveAndReturnResponse(){
        // Arrange
        String ownerEmail = "ofchanataxi@espe.edu.ec";
        double initial = 100.0;

        when(walletRepository.existsByOwnerEmail(ownerEmail)).thenReturn(Boolean.FALSE);
        when(walletRepository.save(Mockito.any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);

        //Act
        WalletResponse response = walletService.createWallet(ownerEmail, initial);

        //Assert
        assertNotNull(response.getWalletId());
        assertEquals(100.0, response.getBalance());

        verify(riskClient).isBloqued(ownerEmail);
        verify(walletRepository).save(Mockito.any(Wallet.class));
        verify(walletRepository).existsByOwnerEmail(ownerEmail);
    }

    @Test
    void createWallet_invalidEmail_shouldThrow_andNotCallDependencies(){
        //Arrange
        String invalidEmail = "ofchanataxiespe.edu.ec";

        //Act + Assert
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(invalidEmail, 50.0));

        //No debe llamar a ninguna dependencia porque falla la validacion
        verifyNoInteractions(walletRepository, riskClient);
    }

    @Test
    void deposit_walletNotFound_shouldThrow(){
        //Arrange
        String walletId = "nonexistent-wallet-id";

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        //Act + Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> walletService.deposit(walletId, 50.0));

        assertEquals("Wallet not found", exception.getMessage());
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).save(Mockito.any(Wallet.class));
    }

    @Test
    void deposit_shouldUpdateBalance_andSave_usingCaptor(){
        double newBalance = walletService.deposit(walletId, 50.0);

        //Assert
        assertEquals(50.0, newBalance);

        verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();
        assertEquals(150.0, saved.getBalance());
    }
}
