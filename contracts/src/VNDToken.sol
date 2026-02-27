// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/// @title VNDToken — Vietnamese Dong Stablecoin (ERC-20 + EIP-2612 Permit)
/// @notice decimals() = 0 because VND has no fractional units.
///         Owner can mint test tokens; intended for Sepolia Testnet.
contract VNDToken is ERC20, ERC20Permit, Ownable {
    constructor(address initialOwner)
        ERC20("VND Stablecoin", "VNDS")
        ERC20Permit("VND Stablecoin")
        Ownable(initialOwner)
    {}

    /// @dev VND has no fractional units — override decimals to 0.
    function decimals() public pure override returns (uint8) {
        return 0;
    }

    /// @notice Mint VNDS tokens. Only callable by contract owner (relayer/admin).
    /// @param to     Recipient address
    /// @param amount Amount in whole VND (no decimals)
    function mint(address to, uint256 amount) external onlyOwner {
        _mint(to, amount);
    }
}
