package com.example.grpcservertest1;

import com.test.grpc.bidir.BiDirServiceGrpc;
import com.test.grpc.bidir.HelloResponse;
import com.test.grpc.first.FirstServiceGrpc;
import com.test.grpc.first.HelloReply;
import com.test.grpc.first.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class GrpcServerTest1Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(GrpcServerTest1Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        start();
        blockUntilShutdown();
    }

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new FirstServiceImpl())
                .addService(new BiDirServiceImpl())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.out.println("*** shutting down gRPC server since JVM is shutting down");
                GrpcServerTest1Application.this.stop();
                System.out.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class FirstServiceImpl extends FirstServiceGrpc.FirstServiceImplBase {

        @Override
        public void test(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    static class BiDirServiceImpl extends BiDirServiceGrpc.BiDirServiceImplBase {

        AtomicInteger counter = new AtomicInteger();

        @Override
        public StreamObserver<com.test.grpc.bidir.HelloRequest> bidiHello(
                StreamObserver<HelloResponse> responseObserver) {
            return new StreamObserver<com.test.grpc.bidir.HelloRequest>() {
                @Override
                public void onNext(com.test.grpc.bidir.HelloRequest helloRequest) {
                    String answer="ky ky " + counter.incrementAndGet()+helloRequest.getName();
                    System.out.println("onNext from server : "+answer);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    responseObserver.onNext(HelloResponse.newBuilder()
                            .setMessage(answer).build());
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("error", throwable);
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
