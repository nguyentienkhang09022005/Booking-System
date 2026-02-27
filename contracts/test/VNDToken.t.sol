// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Test.sol";
import "../src/VNDToken.sol";

contract VNDTokenTest is Test {
    VNDToken internal token;
    address internal owner;
    address internal alice = address(0xA11CE);
    address internal bob   = address(0xB0B);

    function setUp() public {
        owner = address(this);
        token = new VNDToken(owner);
    }

    // ─── Basic ERC-20 ─────────────────────────────────────────────────────────

    function test_Name() public view {
        assertEq(token.name(), "VND Stablecoin");
    }

    function test_Symbol() public view {
        assertEq(token.symbol(), "VNDS");
    }

    function test_Decimals_IsZero() public view {
        assertEq(token.decimals(), 0);
    }

    function test_InitialSupply_IsZero() public view {
        assertEq(token.totalSupply(), 0);
    }

    // ─── Minting ──────────────────────────────────────────────────────────────

    function test_Mint_ByOwner() public {
        token.mint(alice, 1_000_000);
        assertEq(token.balanceOf(alice), 1_000_000);
        assertEq(token.totalSupply(), 1_000_000);
    }

    function test_Mint_Multiple() public {
        token.mint(alice, 500_000);
        token.mint(bob, 300_000);
        assertEq(token.totalSupply(), 800_000);
    }

    function test_RevertIf_MintByNonOwner() public {
        address attacker = address(0xBAD);
        vm.prank(attacker);
        vm.expectRevert();
        token.mint(alice, 1_000_000);
    }

    function test_RevertIf_MintToZeroAddress() public {
        vm.expectRevert();
        token.mint(address(0), 1_000_000);
    }

    // ─── Transfer ─────────────────────────────────────────────────────────────

    function test_Transfer() public {
        token.mint(alice, 1_000_000);
        vm.prank(alice);
        token.transfer(bob, 400_000);
        assertEq(token.balanceOf(alice), 600_000);
        assertEq(token.balanceOf(bob), 400_000);
    }

    function test_RevertIf_TransferInsufficientBalance() public {
        token.mint(alice, 100);
        vm.prank(alice);
        vm.expectRevert();
        token.transfer(bob, 101);
    }

    // ─── Approve / TransferFrom ───────────────────────────────────────────────

    function test_Approve_AndTransferFrom() public {
        token.mint(alice, 1_000_000);
        vm.prank(alice);
        token.approve(bob, 500_000);

        assertEq(token.allowance(alice, bob), 500_000);

        vm.prank(bob);
        token.transferFrom(alice, bob, 300_000);

        assertEq(token.balanceOf(alice), 700_000);
        assertEq(token.balanceOf(bob), 300_000);
        assertEq(token.allowance(alice, bob), 200_000);
    }

    function test_RevertIf_TransferFromExceedsAllowance() public {
        token.mint(alice, 1_000_000);
        vm.prank(alice);
        token.approve(bob, 100);

        vm.prank(bob);
        vm.expectRevert();
        token.transferFrom(alice, bob, 101);
    }

    // ─── EIP-2612 Permit ──────────────────────────────────────────────────────

    function test_Permit() public {
        uint256 privKey = 0xCAFE;
        address signer = vm.addr(privKey);
        token.mint(signer, 1_000_000);

        uint256 value    = 500_000;
        uint256 deadline = block.timestamp + 3600;
        uint256 nonce    = token.nonces(signer);

        bytes32 domainSep = token.DOMAIN_SEPARATOR();
        bytes32 permitHash = keccak256(abi.encodePacked(
            "\x19\x01",
            domainSep,
            keccak256(abi.encode(
                keccak256("Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)"),
                signer,
                bob,
                value,
                nonce,
                deadline
            ))
        ));

        (uint8 v, bytes32 r, bytes32 s) = vm.sign(privKey, permitHash);

        token.permit(signer, bob, value, deadline, v, r, s);

        assertEq(token.allowance(signer, bob), value);
        assertEq(token.nonces(signer), 1);
    }

    function test_Permit_RevertAfterDeadline() public {
        uint256 privKey  = 0xCAFE;
        address signer   = vm.addr(privKey);
        uint256 deadline = block.timestamp - 1; // expired

        bytes32 permitHash = keccak256(abi.encodePacked(
            "\x19\x01",
            token.DOMAIN_SEPARATOR(),
            keccak256(abi.encode(
                keccak256("Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)"),
                signer, bob, 100, token.nonces(signer), deadline
            ))
        ));

        (uint8 v, bytes32 r, bytes32 s) = vm.sign(privKey, permitHash);
        vm.expectRevert();
        token.permit(signer, bob, 100, deadline, v, r, s);
    }

    // ─── Fuzz tests ───────────────────────────────────────────────────────────

    function testFuzz_Mint_And_Transfer(address recipient, uint128 amount) public {
        vm.assume(recipient != address(0));
        vm.assume(amount > 0);
        token.mint(address(this), amount);
        token.transfer(recipient, amount);
        assertEq(token.balanceOf(recipient), amount);
    }
}
