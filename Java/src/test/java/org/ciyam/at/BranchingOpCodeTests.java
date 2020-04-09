package org.ciyam.at;

import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.OpCode;
import org.ciyam.at.test.ExecutableTest;
import org.junit.Test;

public class BranchingOpCodeTests extends ExecutableTest {

	@Test
	public void testBackwardsBranch() throws ExecutionException {
		int backwardsAddr = 0x05;
		int forwardAddr = 0x13;

		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(forwardAddr);

		// backwardsAddr:
		assertEquals(backwardsAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// forwardAddr:
		assertEquals(forwardAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) (backwardsAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(1));
	}

	@Test
	public void testOutOfBoundsForwardsBranch() throws ExecutionException {
		int forwardAddr = codeByteBuffer.limit() - 60; // enough room for post-jump code

		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(forwardAddr);

		codeByteBuffer.position(forwardAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) 80); // way after end
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testOutOfBoundsBackwardsBranch() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) -80); // way before start
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testBZR_DATtrue() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(1));
	}

	@Test
	public void testBZR_DATfalse() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(1));
	}

	@Test
	public void testBNZ_DATtrue() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(1));
	}

	@Test
	public void testBNZ_DATfalse() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(1));
	}

	@Test
	public void testBGT_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGT_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBLT_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLT_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBGE_DATtrue1() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGE_DATtrue2() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBLE_DATtrue1() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLE_DATtrue2() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBEQ_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BEQ_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBEQ_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BEQ_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBNE_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBNE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

}
