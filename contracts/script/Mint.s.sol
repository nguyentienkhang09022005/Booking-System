// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Script.sol";
import "../src/VNDToken.sol";

/// @notice Mints test VNDS tokens to a specified address.
///         Run: RECIPIENT=0x... AMOUNT=1000000000 forge script script/Mint.s.sol --rpc-url sepolia --broadcast
contract Mint is Script {
    function run() external {
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address vndTokenAddress = vm.envAddress("VND_TOKEN_ADDRESS");
        address recipient = vm.envAddress("RECIPIENT");
        uint256 amount = vm.envUint("AMOUNT");

        vm.startBroadcast(deployerPrivateKey);

        VNDToken vndToken = VNDToken(vndTokenAddress);
        vndToken.mint(recipient, amount);

        vm.stopBroadcast();

        console.log("Minted", amount, "VNDS to", recipient);
    }
}
