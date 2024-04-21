package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.link.Link;
import io.reticulum.constant.LinkConstant;
import io.reticulum.packet.Packet;
import io.reticulum.packet.PacketContextType;
import io.reticulum.packet.PacketType;
import io.reticulum.packet.PacketReceipt;
import io.reticulum.packet.PacketReceiptStatus;
import io.reticulum.transport.AnnounceHandler;
import static io.reticulum.link.TeardownSession.DESTINATION_CLOSED;
import static io.reticulum.link.TeardownSession.INITIATOR_CLOSED;
import static io.reticulum.link.TeardownSession.TIMEOUT;
import static io.reticulum.identity.IdentityKnownDestination.recall;
import io.reticulum.utils.IdentityUtils;
import static io.reticulum.constant.ReticulumConstant.TRUNCATED_HASHLENGTH;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.FALSE;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.encoders.UTF8;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;

@Slf4j
public class LinkApp {
    private static final String APP_NAME = "example_utilities";
    Reticulum reticulum;
    static final String defaultConfigPath = new String(".reticulum");
    Identity server_identity;
    Transport transport;
    public Destination destination1, destination2;
    public Link latestClientLink;
    public Link serverLink;
    public Link clientLink;
    
    /************/
    /** Server **/
    /************/
    private void server_setup() {
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

        // We create a destination that clients can connect to. We
        // want clients to create links to this destination, so we
        // need to create a "single" destination type.
        destination1 = new Destination(
            server_identity,
            Direction.IN,
            DestinationType.SINGLE,
            APP_NAME,
            "linkexample"
        );
        log.info("destination1 hash: "+destination1.getHexHash());

        // We configure a function that will get called every time
        // a new client creates a link to this destination.
        destination1.setLinkEstablishedCallback(this::clientConnected);

        //// create a custom announce handler instance
        //var announceHandler = new ExampleAnnounceHandler();
        //// register announce handler
        //transport = Transport.getInstance();
        //transport.registerAnnounceHandler(announceHandler);
        //log.debug("announce handlers: {}", transport.getAnnounceHandlers());

        serverLoop(destination1);
    }

    public void serverLoop(Destination destination) {
        var inData = new String();
        log.info("***> Link server * {} * running, waiting for a connection", destination.getHexHash());

        log.info("Hit enter to manually send an announce (Ctrl-C to quit)");

        Scanner scan = new Scanner( System.in );
        while (true) {
            try {
                inData = scan.nextLine();
                destination.announce();
                log.info("Sent announce from {} ({})", destination.getHexHash(), destination.getName());
            } catch (Exception e) {
                scan.close();
            }
        }
    }

    public void clientConnected(Link link) {
        log.info("Client connected");
        link.setLinkClosedCallback(this::clientDisconnected);
        link.setPacketCallback(this::serverPacketReceived);
        latestClientLink = link;
        log.info("***> Client connected");
    }

    public void clientDisconnected(Link link) {
        log.info("***> Client disconnected");
    }

    public void serverPacketReceived(byte[] message, Packet packet) {
        String text = new String(message, StandardCharsets.UTF_8);
        log.info("Received data on the link: {}", text);
        // send reply
        String replyText = "I received \""+text+"\" over the link";
        byte[] replyData = replyText.getBytes(StandardCharsets.UTF_8);
        Packet reply = new Packet(latestClientLink, replyData, PacketType.DATA);
        reply.send();
    }

    //private class ExampleAnnounceHandler implements AnnounceHandler {
    //    @Override
    //    public String getAspectFilter() {
    //        log.info("getAspectFilter called.");
    //        //return APP_NAME;
    //        return null;
    //    }
    //    
    //    @Override
    //    public void receivedAnnounce(byte[] destinationHash, Identity announcedIdentity, byte[] appData) {
    //        log.info("Received an announce from {}", Hex.encodeHexString(destinationHash));
    //        //log.info("Received an announce from (raw) {}", destinationHash);
    //        
    //        if (appData != null) {
    //            log.info("The announce contained the following app data: {}", new String(appData));
    //        }
    //    }
    //}

    /************/
    /** Client **/
    /************/
    private void client_setup(byte[] destinationHash) {
        try {
            reticulum = new Reticulum(defaultConfigPath);
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }

        Integer destLen = (TRUNCATED_HASHLENGTH / 8) * 2;  // hex characsters
        log.debug("destLen: {}, destinationHash length: {}, floorDiv: {}", destLen, destinationHash.length, Math.floorDiv(destLen,2));
        if (Math.floorDiv(destLen, 2) != destinationHash.length) {
            log.info("Destination length ({} byte) is invalid, must be {} (hex) hexadecimal characters ({} bytes)", destinationHash.length, destLen, Math.floorDiv(destLen,2));
            throw new IllegalArgumentException("Destination length is invalid");
        }

        String inData;
        Identity serverIdentity;
        Destination serverDestination;
        Link link;
        //Scanner scan = new Scanner( System.in );

        // check if we know the destination
        if (isFalse(Transport.getInstance().hasPath(destinationHash))) {
            log.info("Destination is not yet known. Requesting path and waiting for announce to arrive...");
            Transport.getInstance().requestPath(destinationHash);
            while (isFalse(Transport.getInstance().hasPath(destinationHash))) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    log.info("sleep interrupted: {}", e);
                }
            }
        }

        //log.info("Echo client ready, hit enter to establish Link to {} (Ctrl-C to quit)", Hex.encodeHexString(destinationHash));
        //inData = scan.nextLine();
        // recall server identity and inform user that we'll begin connecting
        serverIdentity = recall(destinationHash);
        log.debug("client - serverIdentity: {}", serverIdentity);

        log.info("Establishing link with server...");

        // When the server identity is known, we set up a destination
        serverDestination = new Destination(
            serverIdentity,
            Direction.OUT,
            DestinationType.SINGLE,
            APP_NAME,
            "linkexample"
        );

        log.info("server destination (recalled on client, direction.OUT): * {} *", serverDestination.getHexHash());

        // and create a link
        link = new Link(serverDestination);

        // we set a callback that will be executed
        // every time a packet is received over the link
        link.setPacketCallback(this::clientPacketReceived);

        // We'll also set up functions to inform the user
        // when the link is established or closed
        link.setLinkEstablishedCallback(this::linkEstablished);
        link.setLinkClosedCallback(this::linkClosed);
        
        // Everything is set up, so let's enter a loop
        // for the user to interact with the example
        clientLoop();

    }

    public void clientLoop() {

        // wait for link to become active (maybe give some feedback)
        while (isNull(serverLink)) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                log.info("sleep interrupted: {}", e);
            }
        }

        var shouldQuit = false;
        String inData;
        Packet cPacket;
        while (isFalse(shouldQuit)) {
            try {
                Scanner scan = new Scanner( System.in );
                inData = scan.nextLine();
                System.out.println("You entered: " + inData );
                
                // check if we should quit the example
                if (inData.equalsIgnoreCase("quit")) {
                    shouldQuit = true;
                    serverLink.teardown();
                }
                
                if (inData.equals("quit")) {
                    shouldQuit = true;
                    serverLink.teardown();
                    continue;
                }
                
                if (! inData.isEmpty()) {
                    var data = inData.getBytes(UTF_8);
                    if (data.length <= LinkConstant.MDU) {
                        Packet testPacket = new Packet(serverLink, data, PacketType.DATA);
                        testPacket.send();
                    } else {
                        log.info("Cannot send this packet, the data length of {} bytes exceeds link MDU of {} bytes", data.length, LinkConstant.MDU);
                    }
                }
                scan.close();
            } catch (Exception e) {
                log.error("Error sending data over the link: {}", e);
                shouldQuit = true;
                serverLink.teardown();
            }
        }
    }

    public Runnable linkEstablished() {
        // reference to link was set in client_setup
        Runnable run = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
            }
        };

        // inform the user that the server is connected
        log.info("Link established with server, enter some text to send, or \"quit\" to quit");

        return run;
    }

    public void linkClosed(Link link) {
        if (link.getTeardownReason() == TIMEOUT) {
            log.info("The link timed out, exiting now");
        } else if (link.getTeardownReason() == DESTINATION_CLOSED) {
            log.info("The link was closed by the server, exiting now");
        } else {
            log.info("Link closed, exiting now");
        }

        reticulum.exitHandler();
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException e) {
            log.info("sleep interrupted: {}", e);
        }
    }

    public void clientPacketReceived(byte[] message, Packet packet) {
        String text = new String(message, StandardCharsets.UTF_8);
        log.info("Received data on the link: {}", text);
    }

    /*********/
    /** Main */
    /*********/
    public static void main(String[] args) throws IOException {
        // main to run application directly
        
        String cmdUsage = new String("run_echo.sh [-s|-c HASH]");
        
        // define options
        Options options = new Options();
        //Option o_server = new Option("s", "server", false, "server mode - wait for incoming packets from clients");
        Option o_server = Option.builder("s").longOpt("server")
                            .argName("destination")
                            .hasArg(false)
                            .required(false)
                            .desc("server mode - wait for incoming packets from clients")
                            .build();
        options.addOption(o_server);
        Option o_client = Option.builder("c").longOpt("client")
                            .argName("destination")
                            .hasArg(true)
                            .required(false)
                            .desc("client mode (specify a destination hash)")
                            .build();
        options.addOption(o_client);
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
        
        try {
            cLine = parser.parse(options, args);
        
            if (cLine.hasOption("s")) {
                System.out.println("server mode");
            }
            if (cLine.hasOption("c")) {
               System.out.println("client to destionation: " + cLine.getOptionValue("c"));
            }
        
            var instance = new LinkApp();
            if (cLine.hasOption("c")) {
                try {
                    instance.client_setup(Hex.decodeHex(cLine.getOptionValue("c")));
                } catch (IllegalArgumentException e) {
                    log.error("Invalid destination entered. Check your input!");
                    System.exit(0);
                }
            } else if (cLine.hasOption("s")) {
                instance.server_setup();
            } else {
                formatter.printHelp(cmdUsage, options);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            formatter.printHelp(cmdUsage, options);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }


        //// code without command line parser
        //var instance = new LinkApp();
        //if ("s".equals(args[0])) {
        //    instance.server_setup();
        //} else if ("c".equals(args[0])) {
        //    if (args.length <= 1) {
        //        log.info("number of args entered: {}", args.length);
        //        System.out.println("Usage: run_link.sh c <destination_hash>");
        //    } else {
        //        log.info("client - cli inputs: {}, {}", args[0], args[1]);
        //        try {
        //            //log.info("client - decoded hex sting input[1]: {}", Hex.decodeHex(args[1]));
        //            instance.client_setup(Hex.decodeHex(args[1]));
        //        } catch (DecoderException e) {
        //            log.error("DecoderException: {}", e.fillInStackTrace());
        //        }
        //    }
        //} else {
        //    System.out.println("Usage (server): run_link.sh s");
        //    System.out.println("Usage (client): run_link.sh c <destination_hash>");
        //    System.out.println("'c': client or 's': server (listening mode)");
        //}
    }

}
