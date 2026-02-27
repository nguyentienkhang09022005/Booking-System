// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Script.sol";
import "../src/VNDToken.sol";
import "../src/PaymentGateway.sol";

/// @notice Deploys VNDToken then PaymentGateway to Sepolia.
///         Run: forge script script/Deploy.s.sol --rpc-url sepolia --broadcast --verify
contract Deploy is Script {
    function run() external {
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        vm.startBroadcast(deployerPrivateKey);

        // 1. Deploy VNDToken — deployer is initial owner (can mint)
        VNDToken vndToken = new VNDToken(deployer);
        console.log("VNDToken deployed at:", address(vndToken));

        // 2. Deploy PaymentGateway — pass VNDToken address and deployer as owner
        PaymentGateway gateway = new PaymentGateway(address(vndToken), deployer);
        console.log("PaymentGateway deployed at:", address(gateway));

        vm.stopBroadcast();

        console.log("=== Deployment Complete ===");
        console.log("VND_TOKEN_ADDRESS=", address(vndToken));
        console.log("PAYMENT_GATEWAY_ADDRESS=", address(gateway));
    }
}
