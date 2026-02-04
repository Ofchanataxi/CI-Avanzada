package ec.edu.espe.buildtestci;

import ec.edu.espe.buildtestci.dto.WalletResponse;
import ec.edu.espe.buildtestci.model.Wallet;
import ec.edu.espe.buildtestci.repository.WalletRepository;
import ec.edu.espe.buildtestci.service.RiskClient;
import ec.edu.espe.buildtestci.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    void createWallet_validData_shouldSaveAndReturnResponse() {
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
    void createWallet_invalidEmail_shouldThrow_andNotCallDependencies() {
        //Arrange
        String invalidEmail = "ofchanataxiespe.edu.ec";

        //Act + Assert
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(invalidEmail, 50.0));

        //No debe llamar a ninguna dependencia porque falla la validacion
        verifyNoInteractions(walletRepository, riskClient);
    }

    @Test
    void deposit_walletNotFound_ShouldThrow() {
        //Arrange
        String walletId = "no-exist-wallet";

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        //Act + Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> walletService.deposit(walletId, 60));

        assertEquals("Wallet not found", exception.getMessage());
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void deposit_shouldUpdateBalance_andSave_UsingCaptor() {
        //Arrange
        Wallet wallet = new Wallet("oscar@espe.edu.ec", 300.0);
        String walletId = wallet.getId();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);

        //Act
        double newBalance = walletService.deposit(walletId, 300.0);

        //Assert
        assertEquals(600.0, newBalance);
        verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();
        assertEquals(600.0, saved.getBalance());
    }

    @Test
    void withdraw_insufficientFunds_ShouldThrow_andNotSave() {
        //Arrange
        Wallet wallet = new Wallet("oscar@espe.edu.ec", 300.0);
        String walletId = wallet.getId();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        //Act + Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> walletService.withdraw(walletId, 400.0));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(walletRepository, never()).save(any());

    }

    @Test
    void withdraw_validAmount_shouldUpdateBalance_andSave() {
        // Arrange: Preparamos una cuenta con saldo suficiente (500.0)
        Wallet wallet = new Wallet("ofchanataxi@espe.edu.ec", 500.0);
        String walletId = wallet.getId();
        double withdrawAmount = 200.0;

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        // Simulamos que el repositorio guarda correctamente
        when(walletRepository.save(Mockito.any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        // Act: Ejecutamos el retiro
        double currentBalance = walletService.withdraw(walletId, withdrawAmount);

        // Assert: Verificamos que el saldo bajó a 300 y que se llamó a guardar
        assertEquals(300.0, currentBalance); // 500 - 200 = 300
        verify(walletRepository).save(wallet);
    }
}