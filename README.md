# concurrency-akka-actor-model
practical-java-concurrency-with-the-akka-actor-model

Akka
=====

1. Why do we need Akka?
   Why concurrent programming is difficult in Java.
   Risk of interrupted exception.
   Risk of ConcurrentModificationException.
   Make shared data thread safe.
   Thread blocking.

2. The Actor Model
   The concepts of actor design pattern

   What is an actor?
   Actor is an object that has key features
   name - name of the actor
   path - location of the actor. It helps to uniquely identify each actor.
   Message Queue - When one actor tells another actor something, the message is put into that actors queue.
   Behavior - It will take messages out of the queue one by one and process it. It defines how the actor will process the messgages like whether to ignore the messages, or send response back to the caller actor, or run some code.
   Note: Actors can send or receive messages are not just going to be String or numbers, they can be any class which is serializable.

   Why does this model work from a thread safety point of view?
   Each actor has its own thread.
   Actors won't share data with other actors.
   Messages must be immutable.
   Messages are processed one at a time. (No chance of interruption or InterruptedException)
   Messages are processed one by one. No risk of ConcurrentModificationException as no two different pieces of code need to run code on the same object at the same time.


3. Creating our first actor.
   Creating actor.
   AbstractBehavior class.
   createReceive method
   newReceiveBuilder object
   Defining behavior
   Behaviors.setup(FirstSimpleBehavior::new)
   Instantiating actors and sending messages.
   ActorySystem class
   tell method

4. Going further with actors
   Expanding the receiveBuilder
   onMessageEquals method
   onAnyMessageMethod
   Get path of an actor (getContext().getSelf().path())
   Creating child actors
   ActorRef<String> secondActor = getContext().spawn(FirstSimpleBehavior.create(), "secondActor");
   ActorRef interface
   Actor Paths
   My path is akka://FirstActorSystem/user (User guardian)
   My path is akka://FirstActorSystem/user/secondActor
   Actor name cannot contain spaces. It can include uppercase and lowercase characters, numbers, hyphen(-) or underscores (_).

5. Going further with messages
   Creating a custom message type
   Messages that an actor can send or receive can be any Java class that is serializable.
   Using interfaces
   See the big prime example using akka
   Understanding message delivery guarantees
   Messages are delivered at most once
   Message order is guaranteed between 2 actors.

6. Case Study 1
   We shouldn't use Thread.sleep() in akka. It can throw InterruptedException.

   	SchedulingTimers in Akka.
   		Syntax:
   			return Behaviors.withTimers(timers -> {
                        timers.startTimerAtFixedRate(key, instruction, frequency);
                        return this;
                    });

                key - It's the timer id.
                instruction - The message command which needs to be processed by Actor.
                frequency - How often this instruction needs to be executed.

   		Example:
   			return Behaviors.withTimers(timer -> {
                        timer.startTimerAtFixedRate(TIMER_KEY, new GetPositionsCommand(), Duration.ofSeconds(1));
                        return this;
                    });

7. Going further with behaviors
   Actor behaviors can change over time.
   In the createReceive method we can return Behaviors.same() instead of this.
   We can write multiple handler methods for messages.

8. Actor lifecycles
   Actors stopping themselves
   return Behaviors.stopped() : This is not a great idea to stop the actors. This shouldn't be called from child actors.
   Dead letters encountered log messages.
   Stopping child actors
   return Behaviors.ignore() : This actor is still alive but will not process any further messages. The messaged will be received by this actor and ignored/discarded instead of going to dead letter.

   		Parent actor stopping child actor
   			getContext().stop(racer) : Here, racer is the reference to child actor.

   		Cancelling all timer and stopping the parent actor.
   			return Behaviors.withTimers(timers -> {
                        timers.cancelAll();
                        return Behaviors.stopped();
                    });

        Actor lifecycle methods
        	Stopping an actor will stop all its child actor as well.
        	When an actor stops, it sends a signal called PostStop to the child actors which can be used to clean/close resources in the child actor.

9. Logging
   Logging messages from actors
   getContext().getLog().info("Your log message.");

   	Configuring log level output
   		Configure the xml file for logging.

10. Case study 2 - Blockchain mining example
    A simple introduction to blockchains
    Blockchain is a shared, immutable digital ledger, enabling the recording of transactions and the tracking of assets within a business network and providing a single source of truth.
    Blockchain operates as a decentralized distributed database, with data stored across multiple computers, making it resistant to tampering. Transactions are validated through a consensus mechanism, ensuring agreement across the
    network.
    In blockchain technology, each transaction is grouped into blocks, which are then linked together, forming a secure and transparent chain. This structure guarantees data integrity and provides a tamper-proof record, making blockchain ideal for applications like cryptocurrencies and supply chain management.


11. Unit Testing
    Unit testing with log messages
    Check the MiningTests class.
    BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
    //.....
    testActor.run(message);

    	Unit testing with response messages
    		Check the MiningTests class.
    			TestInbox<HashResult> testInbox = TestInbox.create();
    			WorkerBehavior.Command message = new WorkerBehavior.Command(block, 4385000, 5, testInbox.getRef());
        		testActor.run(message);
    			// .....
    			testInbox.expectMessage(expectedHashResult);

12. Akka Interaction patterns
    The tell and forget and the request-response pattern
    Fire and Forget pattern
    Request - Response pattern (It's actually Request - Adapted Response pattern)

    	The Ask pattern
    		In this pattern the sender expect to receive a single reply to their message from the recipient within a given timeframe. This has the added benefit that the actor sending the message will get to know that the message they sent has definitely been delivered and processed because if it doesn't receive response within the given time period then some kind of time out will occur. That will be like an error. So, we will know in code that the message has either not been delivered or if it has then something went wrong or the receipt hasn't come back.

    		Check ManagerBehavior class in AkkaBigPrimes project for implementation.
    			getContext().ask(Command.class, worker, Duration.ofSeconds(5),
                (me) -> new WorkerBehavior.Command("start", me), // me is nothing but getContext().getSelf()
                (response, throwable) -> {
                    if (response != null) {
                        return response;
                    } else {
                        System.out.println("Worker " + worker.path() + " failed to respond.");
                        return new NoResponseReceivedCommand(worker);
                    }
                });

        Getting data out of akka
        	We can use the Ask Pattern to get the data out of Akka, even outside any actor like the main method.
        	Check the Main class in  AkkaBigPrimes project.
        		CompletionStage<SortedSet<BigInteger>> result = AskPattern.ask(bigPrimes,
                (me) -> new ManagerBehavior.InstructionCommand("start", me),
                Duration.ofSeconds(30),
                bigPrimes.scheduler());

                result.whenComplete(
                (reply, failure) -> {
                    if (reply != null) {
                        reply.forEach(System.out::println);
                    } else {
                        System.out.println("The system didn't respond in time.");
                    }
                    bigPrimes.terminate();
                });


13. Actor Supervision
    Watching actors (supervision)

    		Watching other or child actors
    			getContext().watch(worker);

    		DeathPactException
    			DeathWatch:
    				In Akka, actors can "watch" other actors using context.watch(targetActorRef). This establishes a "DeathWatch" relationship. When the watched actor terminates (stops), the watching actor receives a special Terminated message.
    			DeathPactException Trigger:
    				If an actor receives a Terminated message for a watched actor and its receive method does not have a case to handle this specific Terminated message, Akka's default supervision strategy for DeathPactException is to stop the actor throwing it. This effectively crashes the watching actor.
    			Purpose:
    				DeathPactException serves as a mechanism to enforce the handling of Terminated messages. It ensures that actors watching others are aware of and react to the termination of their watched counterparts, preventing silent failures or unexpected behavior in the system. While it indicates a lack of explicit handling, it's not always a programming error; sometimes, letting the watching actor stop upon the watched actor's termination is the desired behavior.

    		Handling Terminated message in the actor which is watching to avoid DeathPactException.
    			return newReceiveBuilder()
                .onSignal(Terminated.class, handler -> {
                    return Behaviors.same();
                })
                .....

    	Dealing with actors that crash
    		One is like above handling DeathPactException.
    		Other way is to Add a supervision strategy while creating the child/other actor.
    			Behavior<WorkerBehavior.Command> workerBehavior =
                Behaviors.supervise(WorkerBehavior.create()).onFailure(SupervisorStrategy.resume());
        		ActorRef<WorkerBehavior.Command> worker = getContext().spawn(workerBehavior, "worker" + currentNonce);

        Shutting down all the child actors
        	for (ActorRef<Void> child: getContext().getChildren()) {
                        getContext().stop(child);
                    }


14. Production standard techniques
    Ensuring immutable state

    	Actors sending messages to themselves
    		getContext().getSelf().tell(message);

    	Stashing messages
    		Stashing in Akka allows an actor to temporarily set aside messages it cannot immediately process. It is a powerful tool for managing state transitions, where an actor must wait for a specific condition or a different state before it can handle certain messages. Once the actor is ready, it can "unstash" the messages, returning them to the front of its mailbox for processing. 

    		Creating actor with stashbuffer
    			public static Behavior<Command> create() {
       				return Behaviors.withStash(10,
                		stash -> {
                    		return Behaviors.setup(context -> {
                        		return new ManagerBehavior(context, stash);
                    		});
                		});
    			} 

    	Using routers for simultaneous actor operations
    		In Akka, a router is a specialized actor designed to distribute messages to a set of other actors, known as "routees". Using routers enables you to parallelize work and balance the load across multiple actors, all of which look and behave like a single entity to the message sender. 
    		Routers are highly optimized for throughput, with the routing logic embedded directly into their ActorRef, bypassing the typical message-processing pipeline of a normal actor.

    		Check the MiningSystemBehavior class in AkkaBlockChain project.