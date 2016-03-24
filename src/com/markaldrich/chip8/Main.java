package com.markaldrich.chip8;

import com.markaldrich.jgl.JGLGame;
import com.markaldrich.jgl.JGLGameController;
import com.markaldrich.jgl.JGLGameProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.Random;

/**
 * Created by maste on 3/24/2016.
 */
public class Main extends JGLGame {
	
	private JGLGameProperties props;
	private static File file;
	
	public static int[] chip8_fontset = {
		0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
		0x20, 0x60, 0x20, 0x20, 0x70, // 1
		0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
		0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
		0x90, 0x90, 0xF0, 0x10, 0x10, // 4
		0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
		0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
		0xF0, 0x10, 0x20, 0x40, 0x40, // 7
		0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
		0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
		0xF0, 0x90, 0xF0, 0x90, 0x90, // A
		0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
		0xF0, 0x80, 0x80, 0x80, 0xF0, // C
		0xE0, 0x90, 0x90, 0x90, 0xE0, // D
		0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
		0xF0, 0x80, 0xF0, 0x80, 0x80  // F
	};
	
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
	
	// Draw flag
	public boolean drawFlag = false;
	
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
		
		// Update keyboard
		if(checkIfKeyIsDown(KeyEvent.VK_X)) {
			key[0] = 1;
		} else {
			key[0] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_1)) {
			key[1] = 1;
		} else {
			key[1] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_2)) {
			key[2] = 1;
		} else {
			key[2] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_3)) {
			key[3] = 1;
		} else {
			key[3] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_Q)) {
			key[4] = 1;
		} else {
			key[4] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_W)) {
			key[5] = 1;
		} else {
			key[5] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_E)) {
			key[6] = 1;
		} else {
			key[6] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_A)) {
			key[7] = 1;
		} else {
			key[7] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_S)) {
			key[8] = 1;
		} else {
			key[8] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_D)) {
			key[9] = 1;
		} else {
			key[9] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_Z)) {
			key[0xA] = 1;
		} else {
			key[0xA] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_C)) {
			key[0xB] = 1;
		} else {
			key[0xB] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_4)) {
			key[0xC] = 1;
		} else {
			key[0xC] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_R)) {
			key[0xD] = 1;
		} else {
			key[0xD] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_F)) {
			key[0xE] = 1;
		} else {
			key[0xE] = 0;
		}
		if(checkIfKeyIsDown(KeyEvent.VK_V)) {
			key[0xF] = 1;
		} else {
			key[0xF] = 0;
		}
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
			case 0xB000: {
				int nnn = opcode & 0x0FFF;
				pc = nnn + (V[0] & 0xFF);
				break;
			}
			case 0xC000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				V[x] = nn & (new Random().nextInt(256));
				break;
			}
			case 0xD000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				int height = (opcode & 0x000F);
				int pixel;
				
				V[0xF] = 0;
				for(int yLine = 0; yLine < height; yLine++) {
					pixel = read(I + yLine);
					for(int xLine = 0; xLine < 8; xLine++) {
						if((pixel & (0x80 >> xLine)) != 0) {
							if(gfx[(x + xLine + ((y + yLine) * 64))] == 1) {
								V[0xF] = 1;
							}
							gfx[x + xLine + ((y + yLine) * 64)] ^= 1;
						}
					}
				}
				
				drawFlag = true;
				break;
			}
			case 0xE000: {
				int x = (opcode & 0x0F00) >> 8;
				// TODO: There is probably a much simpler way to do this, but who knows...
				if((opcode & 0x00FF) == 0x009E) {
					if(key[x] != 0) {
						pc += 2;
					}
				} else {
					if(key[x] == 0) {
						pc += 2;
					}
				}
				break;
			}
			case 0xF000: {
				int x = (opcode & 0x0F00) >> 8;
				switch(opcode & 0x00FF) {
					case 0x07: {
						V[x] = delayTimer;
						break;
					}
					case 0x0A: {
						int keyPressed = -1;
						loop: while(true) {
							for(int i = 0; i < key.length; i++) {
								if(key[i] != 0) {
									keyPressed = i;
									break loop;
								}
							}
						}
						V[x] = keyPressed;
						break;
					}
					case 0x15: {
						delayTimer = V[x];
						break;
					}
					case 0x18: {
						soundTimer = V[x];
						break;
					}
					case 0x1E: {
						I += V[x];
						break;
					}
					case 0x29: {
						I = V[x] * 5;
						break;
					}
					case 0x33: {
						int value = V[x];
						write(value / 100, I);
						write((value % 100) / 10, I + 1);
						write((value % 100) % 10, I + 2);
						break;
					}
					case 0x55: {
						for(int i = 0; i <= x; i++) {
							write(V[i], I + i);
						}
						break;
					}
					case 0x65: {
						for(int i = 0; i <= x; i++) {
							V[i] = read(I + i);
						}
						break;
					}
				}
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
		return drawFlag;
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
