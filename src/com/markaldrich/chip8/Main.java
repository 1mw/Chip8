package com.markaldrich.chip8;

import com.markaldrich.jgl.JGLGame;
import com.markaldrich.jgl.JGLGameController;
import com.markaldrich.jgl.JGLGameProperties;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;

/**
 * Created by maste on 3/24/2016.
 */
public class Main extends JGLGame {
	
	private JGLGameProperties props;
	private static File file;
	
	/*
	 * Emulator variables
	 */
	// Even though the opcode is a short, it is unsigned, so a Java signed short cannot
	// be used
	public int opcode;
	
	// Main memory of the Chip8
	// Mapped out as:
	/**
	 * 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
	 * 0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
	 * 0x200-0xFFF - Program ROM and work RAM
	 */
	public int[] memory = new int[4096];
	
	// CPU registers
	public int[] V = new int[16];
	
	// Index register
	public int I;
	
	// Program counter
	public int pc;
	
	// Graphics memory
	public int[] gfx = new int[64 * 32];
	
	// Two countdown registers
	public int delayTimer;
	// TODO: The emulator will make a sound when this register gets to 0.
	public int soundTimer;
	
	// Stack and stack pointer
	public int[] stack = new int[16];
	public int sp;
	
	// Keyboard memory
	public int[] key = new int[16];
	
	public Main(JGLGameProperties props) {
		super(props);
		this.props = props;
	}
	
	@Override
	public void initGame() {
		// TODO: Set up graphics and input
		pc = 0x200;
		opcode = 0;
		I = 0;
		sp = 0;
		delayTimer = 0;
		soundTimer = 0;
		
		// TODO: Load fontset
		
		byte[] data = null;
		try {
			data = Files.readAllBytes(file.toPath());
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		for(int i = 0; i < data.length; i++) {
			write((int) data[i], i + 0x200);
		}
	}
	
	public void write(int value, int location) {
		if(location > 0x1000 || location < 0) {
			System.out.println("Invalid write! Program tried to write 0x" + Integer.toHexString(value)
				 + " to location 0x" + Integer.toHexString(location));
			System.exit(1);
		} else {
			memory[location] = value & 0xFF;
		}
	}
	
	public int read(int location) {
		if(location > 0x1000 || location < 0) {
			System.out.println("Invalid read! Program tried to read from " + Integer.toHexString(location));
			System.exit(1);
			return 0;
		} else {
			return memory[location] & 0xFF;
		}
	}
	
	@Override
	public void render(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Courier New", Font.PLAIN, 12));
		g.drawString("Test!", 0, 12);
		g.drawString("The name of the rom is " + file.getName(), 0, 24);
		
		if(checkDrawFlag()) {
			// TODO: render the graphics buffer to the screen
		}
	}
	
	@Override
	public void update() {
		// TODO: Use delta timing to actually run the CPU at the correct frequency
		// it is currently running at 60hz
		cycle();
	}
	
	public void cycle() {
		// Fetch opcode
		opcode = memory[pc] << 8 | memory[pc + 1];
		
		// Decode and execute opcode
		switch(opcode & 0xF000) {
			case 0x0000: {
				if((opcode & 0x000F) == 0x0000) {
					// 0x00E0
					// Clear the screen
					for(int i = 0; i < gfx.length; i++) {
						// TODO: is this right?
						gfx[i] = 0;
					}
				} else {
					// 0x00EE
					// TODO: just a guess
					pc = stack[--sp];
				}
			}
			case 0x1000: {
				// TODO: good?
				pc = opcode & 0x0FFF;
				break;
			}
			case 0x2000: {
				stack[sp++] = pc;
				pc = opcode & 0x0FFF;
				break;
			}
			case 0x3000: {
				int registerIndex = (opcode & 0x0F00) >> 8;
				int indexValue = V[registerIndex];
				if((indexValue & 0x00FF) == (opcode & 0x00FF)) {
					// Skip an instruction here, another one will be skipped when this method
					// is over.
					pc += 2;
				}
				break;
			}
			case 0x4000: {
				int registerIndex = (opcode & 0x0F00) >> 8;
				int indexValue = V[registerIndex];
				if((indexValue & 0x00FF) != (opcode & 0x00FF)) {
					// Skip an instruction
					pc += 2;
				}
				break;
			}
			case 0x5000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if(V[x] == V[y]) {
					// Skip an instruction
					pc += 2;
				}
 				break;
			}
			case 0x6000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = opcode & 0x00FF;
				V[x] = nn;
				break;
			}
			case 0x7000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = opcode & 0x00FF;
				V[x] += nn;
				break;
			}
			case 0x8000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				switch(opcode & 0x000F) {
					case 0x0000: {
						V[x] = V[y];
						break;
					}
					case 0x0001: {
						V[x] = V[x] | V[y];
						break;
					}
					case 0x0002: {
						V[x] = V[x] & V[y];
						break;
					}
					case 0x0003: {
						V[x] = V[x] ^ V[y];
						break;
					}
					case 0x0004: {
						V[x] += V[y];
						if(V[x] > 0xFF) {
							// If the resulting value is over 0xFF, or the maximum, set the carry flag
							V[0xF] = 1;
						}
						V[x] &= 0x00FF;
						break;
					}
					case 0x0005: {
						V[0xF] = (V[y] > V[x]) ? 0 : 1;
						V[x] -= V[y];
						V[x] &= 0x00FF;
						break;
					}
					case 0x0006: {
						V[0xF] = V[x] & 0x1;
						V[x] = V[x] >> 1;
						break;
					}
					case 0x0007: {
						V[0xF] = (V[x] > V[y]) ? 0 : 1;
						V[y] -= V[x];
						V[y] &= 0x00FF;
						break;
					}
					case 0x000E: {
						V[0xF] = ((V[x] & 0x80) >> 8);
						V[x] = V[x] << 1;
						break;
					}
				}
				break;
			}
			case 0x9000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if(V[x] != V[y]) {
					pc += 2;
				}
				break;
			}
			case 0xA000: {
				I = opcode & 0x0FFF;
				break;
			}
		}
		
		pc += 2;
		
		
		// Update timers (wow so they actually go down at 60hz which is perfect)
		if(delayTimer > 0) {
			delayTimer--;
		}
		if(soundTimer > 0) {
			if(soundTimer == 1) {
				// TODO: beep
				System.out.println("beep");
			}
			soundTimer--;
		}
	}
	
	public boolean checkDrawFlag() {
		// TODO: Actually check the memory for the draw flag
		return false;
	}
	
	public static void main(String[] args) {
		while (true) {
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog(null);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				break;
			}
		}
		
		JGLGameProperties props = new JGLGameProperties(file.getName(), 800, 600);
		JGLGame game = new Main(props);
		JGLGameController.startGame(game, props);
	}
}
