package Configs

import (
	"fmt"
	"os"
)

var (
	SepoliaRPCURL          string
	RelayerPrivateKey      string
	VNDTokenAddress        string
	PaymentGatewayAddress  string
	ChainID                int64 = 11155111 // Sepolia
)

func BlockchainConfig() error {
	SepoliaRPCURL = os.Getenv("SEPOLIA_RPC_URL")
	if SepoliaRPCURL == "" {
		return fmt.Errorf("SEPOLIA_RPC_URL is required")
	}

	RelayerPrivateKey = os.Getenv("RELAYER_PRIVATE_KEY")
	if RelayerPrivateKey == "" {
		return fmt.Errorf("RELAYER_PRIVATE_KEY is required")
	}

	VNDTokenAddress = os.Getenv("VND_TOKEN_ADDRESS")
	if VNDTokenAddress == "" {
		return fmt.Errorf("VND_TOKEN_ADDRESS is required")
	}

	PaymentGatewayAddress = os.Getenv("PAYMENT_GATEWAY_ADDRESS")
	if PaymentGatewayAddress == "" {
		return fmt.Errorf("PAYMENT_GATEWAY_ADDRESS is required")
	}

	return nil
}
