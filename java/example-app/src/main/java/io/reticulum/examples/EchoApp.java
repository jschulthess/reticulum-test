package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.packet.Packet;
//import io.reticulum.packet.PacketContextType;
//import io.reticulum.packet.PacketType;
import io.reticulum.packet.PacketReceipt;
import io.reticulum.packet.PacketReceiptStatus;
//import io.reticulum.transport.AnnounceHandler;
import static io.reticulum.identity.IdentityKnownDestination.recall;
import io.reticulum.utils.IdentityUtils;
import static io.reticulum.constant.ReticulumConstant.TRUNCATED_HASHLENGTH;
import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

//import static java.nio.charset.StandardCharsets.UTF_8;
//import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
//import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Scanner;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

// for cli options
import org.apache.commons.cli.*;


@Slf4j
public class EchoApp {
    private static final String APP_NAME = "example_utilities";
    Reticulum reticulum;
    Identity server_identity;
    Transport transport;
    public Destination echoDestination;
    //static Logger log = Logger.getLogger(EchoApp.class.getName());
    //private static final Logger log = LoggerFactory.getLogger(EchoApp.class);

    static final  String defaultConfigPath = new String(".reticulum"); // if empty will look in Reticulums default paths
    
    /************/
    /** Server **/
    /************/
    private void server() {

        try {
            reticulum = new Reticulum(defaultConfigPath);
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }

        // create identity either from file or new (creating new keys)
        var serverIdentityPath = reticulum.getStoragePath().resolve("identities/"+APP_NAME);
        if (Files.isReadable(serverIdentityPath)) {
            server_identity = Identity.fromFile(serverIdentityPath);
            log.info("server identity loaded from file {}", serverIdentityPath.toString());
        } else {
            server_identity = new Identity();
            log.info("new server identity created dynamically.");
        }
        //log.debug("Server Identity: {}", server_identity.toString());

        echoDestination = new Destination(
            server_identity,
            Direction.IN,
            DestinationType.SINGLE,
            APP_NAME,
            "echo",
            "request"
        );
        log.info("echoDestination hash: "+echoDestination.getHexHash());

        // We configure the destination to automatically prove all
        // packets addressed to it. By doing this, RNS will automatically
        // generate a proof for each incoming packet and transmit it
        // back to the sender of that packet.
        echoDestination.setProofStrategy(ProofStrategy.PROVE_ALL);

        // Tell the destination which function in our program to
        // run when a packet is received. We do this so we can
        // print a log message when the server receives a request
        echoDestination.setPacketCallback(this::serverCallback);

        // create a custom announce handler instance
        // note: announce handler not strictly necessary for this example
        //var announceHandler = new ExampleAnnounceHandler();
        // register announce handler
        //transport = Transport.getInstance();
        //transport.registerAnnounceHandler(announceHandler);
        //log.debug("announce handlers: {}", transport.getAnnounceHandlers());

        // Everything's ready!
        // Let's Wait for client requests or user input
        announceLoop(echoDestination);
    }

    public void announceLoop(Destination destination) {
        log.info("***> Echo server * {} * running, hit enter to manually send an announce (Ctrl-C to quit)", Hex.encodeHexString(destination.getHash()));
        String inData;

        // We enter a loop that returns until the user exits.
        // If the user hits enter, we will announce our server
        // destination on the network, which will let clients
        // know how to create messages directed towards it.
        while (true) {
            Scanner scan = new Scanner( System.in );
            inData = scan.nextLine();
            destination.announce();
            log.info("Sent announce from {} ({})", Hex.encodeHexString(destination.getHash()), destination.getName());
        }
    }

    public void serverCallback (byte[] message, Packet packet) {

        // Tell the user that we received an echo request, and
        // that we are going to send a reply to the requester.
        // Sending the proof is handled automatically, since we
        // set up the destination to prove all incoming packets.

        var receptionStats = new String("");
        if (reticulum.isConnectedToSharedInstance()) {
            //var receptionRssi = Reticulum.getPacketRssi(packet.getPacketHash());
            //var receptionSnr = Reticulum.getPacketSnr(packet.getPacketHash());
            //if (nonNull(receptionRssi)) {
            //    receptionStats += " [RSSI "+receptionRssi.toString()+" dbm]";
            //}
            //if (nonNull(receptionSnr)) {
            //    receptionStats += " [SNR "+receptionSnr.toString()+" dBm]";
            //}
            log.info("shared instance - stats n/a (not implemented)");
        } else {
            var reception_rssi = packet.getRssi();
            if (nonNull(reception_rssi)) {
                receptionStats += " [RSSI"+reception_rssi.toString()+" dbm]";
                //System.out.println("RSSI "+reception_rssi.toString()+" dBm");
            }
            if (nonNull(packet.getSnr())) {
                receptionStats += " [SNR"+packet.getSnr().toString()+" dB]";
                //System.out.println("RSSI "+packet.getSnr().toString()+" dBm");
            }
            log.info("non-shared instance - reception_rssi: {}", reception_rssi);
        }

        log.info("Received packet from echo client, proof sent {}", receptionStats);
    }

    //private class ExampleAnnounceHandler implements AnnounceHandler {
    //    @Override
    //    public String getAspectFilter() {
    //        log.debug("getAspectFilter called.");
    //        return null;
    //    }
    //    
    //    @Override
    //    public void receivedAnnounce(byte[] destinationHash, Identity announcedIdentity, byte[] appData) {
    //        log.info("Received an announce from {}", Hex.encodeHexString(destinationHash));
    //        
    //        if (appData != null) {
    //            log.info("The announce contained the following app data: {}", new String(appData));
    //        }
    //    }
    //}

    /************/
    /** Client **/
    /************/
    private void client(byte[] destinationHash, Long timeout) {
        
        // We need a binary representation of the destination hash
        // that was entered on the command line
        Integer destLen = (TRUNCATED_HASHLENGTH / 8) * 2;  // hex characsters
        //log.debug("destLen: {}, destinationHash length: {}, floorDiv: {}", destLen, destinationHash.length, Math.floorDiv(destLen,2));
        if (Math.floorDiv(destLen, 2) != destinationHash.length) {
            log.info("Destination length ({} byte) is invalid, must be {} (hex) hexadecimal characters ({} bytes)", destinationHash.length, destLen, Math.floorDiv(destLen,2));
            throw new IllegalArgumentException("Destination length is invalid");
        }

        // We must initialise Reticulum
        try {
            reticulum = new Reticulum(defaultConfigPath);
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }

        log.info("Echo client ready, hit enter to send echo request to {} (Ctrl-C to quit)",
                 Hex.encodeHexString(destinationHash));

        String inData;
        Identity serverIdentity;
        Destination requestDestination;
        Scanner scan = new Scanner( System.in );

        try {
            while (true) {
                inData = scan.nextLine();
                System.out.println("You entered: " + inData );

                // Let's first check if RNS knows a path to the destination.
                // If it does, we'll load the server identity and create a packet
                if (Transport.getInstance().hasPath(destinationHash)) {

                    // To address the server, we need to know it's public
                    // key, so we check if Reticulum knows this destination.
                    // This is done by calling the "recall" method of the
                    // Identity module. If the destination is known, it will
                    // return an Identity instance that can be used in
                    // outgoing destinations.
                    serverIdentity = recall(destinationHash);
                    //log.debug("client - serverIdentity: {}", serverIdentity);

                    // We got the correct identity instance from the
                    // recall method, so let's create an outgoing
                    // destination. We use the naming convention:
                    // example_utilities.echo.request
                    // This matches the naming we specified in the
                    // server part of the code.
                    //log.info("client - destination hash (input): {}", Hex.encodeHexString(destinationHash));
                    requestDestination = new Destination(
                        serverIdentity,
                        Direction.OUT,
                        DestinationType.SINGLE,
                        APP_NAME,
                        "echo",
                        "request"
                    );
                    
                    //log.info("server destination (recalled on client, direction.OUT): * {} *", requestDestination.getHexHash());
                    //log.info("client - sending packet to (server, IN) {} from (client, OUT): {}", Hex.encodeHexString(destinationHash), requestDestination.getHexHash());
                    
                    // The destination is ready, so let's create a packet.
                    // We set the destination to the request_destination
                    // that was just created, and the only data we add
                    // is a random hash.
                    //Packet echoRequest = new Packet(requestDestination, IdentityUtils.getRandomHash(), PacketType.DATA);
                    Packet echoRequest = new Packet(requestDestination, IdentityUtils.getRandomHash());
                    
                    // Send the packet! If the packet is successfully
                    // sent, it will return a PacketReceipt instance.
                    PacketReceipt packetReceipt = echoRequest.send();
                    log.info("aaa - packetReceipt: {}", packetReceipt);
                    
                    if (nonNull(timeout) && (timeout > 0L)) {
                        packetReceipt.setTimeout(timeout);
                        packetReceipt.setTimeoutCallback(this::packetTimedOut);
                    }
                    
                    // We can then set a delivery callback on the receipt.
                    // This will get automatically called when a proof for
                    // this specific packet is received from the destination.
                    packetReceipt.setDeliveryCallback(this::packetDelivered);

                    // Tell the user that the echo request was sent
                    log.info("Sent echo request to {}", Hex.encodeHexString(destinationHash));
                } else {
                    // If we do not know this destination, tell the
                    // user to wait for an announce to arrive.
                    log.info("Destination is not yet known.");
                    log.info("=> Hit enter on the server side to trigger an announcement, then hit enter here again.");
                    Transport.getInstance().requestPath(destinationHash);
                }
            }
        } catch (Exception e) {
            log.info("Exception caught: {}", e);
            scan.close();
        }
    }

    public void packetDelivered(PacketReceipt receipt) {
        var rttString = new String("");
        //log.info("packet delivered callback, receipt: {}", receipt);
        if (receipt.getStatus() == PacketReceiptStatus.DELIVERED) {
            var rtt = receipt.getRtt();    // rtt (Java) is in miliseconds
            //log.info("qqp - packetDelivered - rtt: {}", rtt);
            if (rtt >= 1000) {
                rtt = Math.round(rtt / 1000);
                rttString = String.format("%d seconds", rtt);
            } else {
                rttString = String.format("%d miliseconds", rtt);
            }
            log.info("Valid reply received from {}, round-trip time is {}",
                    Hex.encodeHexString(receipt.getDestination().getHash()), rttString);
        }
        //else {
        //    log.info("NO valid reply, receipt status: {}", receipt.getStatus());
        //}
    }

    public void packetTimedOut(PacketReceipt receipt) {
        if (receipt.getStatus() == PacketReceiptStatus.FAILED) {
            log.info("packet {} timed out", Hex.encodeHexString(receipt.getHash()));
        }
    }


    /*********/
    /** Main */
    /*********/
    public static void main(String[] args) throws IOException {
        // main to run application directly

        String cmdUsage = new String("run_echo.sh [-s|-c HASH [-t TIMEOUT]] ");

        // define options
        Options options = new Options();
        Option o_server = new Option("s", "server", false, "server mode - wait for incoming packets from clients");
        options.addOption(o_server);
        Option o_client = Option.builder("c").longOpt("client")
                            .argName("destination")
                            .hasArg(true)
                            .required(false)
                            .desc("client mode (specify a destination hash)")
                            .build();
        options.addOption(o_client);
        Option o_timeout = Option.builder("t").longOpt("timeout")
                            .argName("timeout")
                            .hasArg(true)
                            .required(false)
                            .desc("set a reply (client mode) timeout in seconds (default: 8s)")
                            .type(Integer.class)
                            .build();
        options.addOption(o_timeout);
        Option o_config = Option.builder("config")
                            .argName("dir")
                            .hasArg(true)
                            .required(false)
                            .desc("(optional) path to alternative Reticulum config directory "
                                  + "(default: .reticulum)").build();
        options.addOption(o_config);
        // define parser
        CommandLine cLine;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        Long defaultTimeout = 8*1000L; // timeout: 8s
        try {
            cLine = parser.parse(options, args);

            if (cLine.hasOption("s")) {
                System.out.println("server mode");
            }
            if (cLine.hasOption("c")) {
               System.out.println("client to destionation: " + cLine.getOptionValue("c"));
            }
            if (cLine.hasOption("t")) {
                Integer t = cLine.getParsedOptionValue("t");
                System.out.println("timeout set to " + t);
            }
            
            var instance = new EchoApp();
            if (cLine.hasOption("c")) {
                try {
                    if (cLine.hasOption("t")) {
                        Integer t = cLine.getParsedOptionValue("t");
                        instance.client(Hex.decodeHex(cLine.getOptionValue("c")), t*1000L);
                    } else {    
                        instance.client(Hex.decodeHex(cLine.getOptionValue("c")), defaultTimeout);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Invalid destination entered. Check your input!");
                    System.exit(0);
                }
            } else if (cLine.hasOption("s")) {
                instance.server();
            } else {
                formatter.printHelp(cmdUsage, options);
            }
        } catch (DecoderException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (ParseException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
            formatter.printHelp(cmdUsage, options);
            System.exit(0);
        }

        //// code without command line parser
        //var instance = new EchoApp();
        //if ("s".equals(args[0])) {
        //    instance.server();
        //    //instance.server_run();
        //} else if ("c".equals(args[0])) {
        //    if (args.length <= 1) {
        //        log.info("number of args entered: {}", args.length);
        //        System.out.println("Usage: run_echo.sh c <destination_hash>");
        //    } else {
        //        log.info("client - cli inputs: {}, {}", args[0], args[1]);
        //        try {
        //            //log.info("client - decoded hex sting input[1]: {}", Hex.decodeHex(args[1]));
        //            instance.client(Hex.decodeHex(args[1]), 8*1000L); // timeout: 8s
        //        } catch (DecoderException e) {
        //            log.error("DecoderException: {}", e.fillInStackTrace());
        //        }
        //    }
        //} else {
        //    System.out.println("Usage (server): run_echo.sh s");
        //    System.out.println("Usage (client): run_echo.sh c <destination_hash>");
        //    System.out.println("'c': client or 's': server (listening mode)");
        //}
    }

}
