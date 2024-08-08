Title:  Assignment 1 - Distributed Systems, University of Adelaide 2024

Overview:
    To gain an understanding of how a distributed system works, this first assignment involves developing a simple Java RMI application. This involved developing both the client and server side of a distributed application: a simple calculator server.

    The calculator server operates a stack and clients push values and operations on to the stack. Each client has its own stack. An operator is pushed only after pushing at least one value and only pops when there is a value on the stack. It assumes that the operator provided will only be one of the four displayed types and that the values are always integers.

Features:
    void pushVal(int val, String clientID):
        -   Pushes a value onto the stack of specified client "clientID".

    void pushOperation(String operator, String clientID):
        -   Pushes a String containing an operator ("min", "max", "lcm", "gcd") to the stack, which will
            cause the server to pop all the values on the stack and:
                -   min: push the min value of all the popped values.
                -   max: push the max value of all the popped values.
                -   lcm: push the least common multiple of all the popped values.
                -   gcd: push the greatest common divisor of all the popped values.

    int pop(String clientID):
        -   Pops the top of the specific stack.

    boolean isEmpty(String clientID):
        -   Returns true if the stack is empty, false otherwise.

    int delayPop(int millis, String clientID):
        -   Waits "millis" milliseconds before carrying out the pop operation as above.

How to run (Manual Usage):
    Manual Usage requires commenting out all JUnit tests, and incorporating desired 
    functionality into the empty main function.
    1.  Open 3 bash shells, 1 each for compilation & rmiregistry, Server and Client.
    2.  Enter "make compile" into first shell to compile all class files.
    3.  Enter "rmiregistry &".
    4.  Switch over to "Server" shell, and execute "make runServer"
    5.  Switch to "Client" shell, and execute "make runClient"
    6.  (OPTIONAL) Run "make clean" to clean up directory.

How to run (Automated Testing):
    1.  Run "make" in shell.
    2.  "tests.log" will contain log file of JUnit testing output.
    3.  (OPTIONAL) Run "make clean" to clean up directory.

Testing:
    Tests have been run automatically with make, utilising JUnit Jupiter (JUnit 5).
    Tests include:
        1.  Testing pushVal() and pop() methods
            -   Simple calling of pushVal() & pop() methods with the value 10, expecting
                10 to be returned, and the message "Push & Pop Test Passed".
        2.  Testing pushOperation() - min, max, lcm, gcd
            -   Pushing 3 values onto the stack, with the appropriate passed argument.
            -   Expecting:
                min:    10
                max:    30
                lcm:    60
                gcd:    10
                invalid:    Exception - Invalid Operator
        3.  Testing delayPop(int millis, String clientID)
            -   Delay of 5 seconds before pop.
        4.  Testing isEmpty(String clientID)
            -   Tested with pushing a value and checking if empty, expecting false.
            -   Tested with popping the previous value, proceeded by checking emptiness.
                Expecting true.
        5.  Multi-Client Testing
            -   Utilising threads to simulate simultaneous client connects, along with
                pre-determined delay in some.
            -   Expecting output client order to be 1st or 4th as first two, followed by 
                5th, 3rd and 2nd clients.

Dependencies:
    JUnit standalone console jar has been used to conduct automated testing.
    The jar is located in the subdirectory "test-lib".