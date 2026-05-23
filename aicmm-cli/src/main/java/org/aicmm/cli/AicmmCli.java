package org.aicmm.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * AiCMM Command-Line Interface.
 * Provides tools for inspecting agents and generating Agent Cards.
 */
@Command(
        name = "aicmm",
        description = "Agent Capability Maturity Model — inspect, score, and classify AI agents",
        version = "0.1.0-SNAPSHOT",
        mixinStandardHelpOptions = true,
        subcommands = {CommandLine.HelpCommand.class}
)
public class AicmmCli implements Runnable {

    @Override
    public void run() {
        System.out.println("""
                ╔══════════════════════════════════════════════════╗
                ║   AiCMM — Agent Capability Maturity Model       ║
                ║   Evaluate · Classify · Govern AI Agents        ║
                ╚══════════════════════════════════════════════════╝
                
                Use --help to see available commands.
                """);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AicmmCli()).execute(args);
        System.exit(exitCode);
    }
}
