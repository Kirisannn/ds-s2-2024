# Chapter 1.5:	Key Challenges of Designing Distributed Systems
---
## 1.5.1	Heterogeneity

Heterogeneity (that is, variety and difference) applies to all of the following:

- networks;
- computer hardware;
- operating systems;
- programming languages;
- implementations by different developers.

Despite the different types of networks on the internet, they are all similar in using the same internet protocols for communication.

*Note:
Although the operating systems of all computers on the Internet need to include an implementation of the Internet protocols, they do not necessarily all provide the same application programming interface to these protocols. For example, the calls for exchanging messages in UNIX are different from the calls in Windows.*

> **Middleware** 
> - **Definition**: Software layer providing programming abstraction and masking heterogeneity of networks, hardware, OS, and programming languages.
> - **Examples**: CORBA (multi-language support), Java RMI (single-language support).
> - **Implementation**: Over Internet protocols, addressing OS and hardware differences.
> - **Functions**: Solves heterogeneity issues, provides a uniform computational model.
> - **Models**: Remote object invocation, event notification, SQL access, transaction processing.
> - **CORBA**: Allows remote object invocation, hides network message passing.

> **Heterogeneity and Mobile Code**
> - **Mobile Code**: Refers to program code that can be transferred and run on different computers (e.g., Java applets).
> - **Compatibility Issues**: Executable programs are specific to the instruction set and host operating system, making them unsuitable for different systems.
> - **Virtual Machine Approach**:
	- Compilers generate code for a virtual machine rather than specific hardware.
	- Example: Java compiler produces code for the Java virtual machine, which interprets and executes it.
	- Java virtual machine needs to be implemented for each type of computer to run Java programs.
> - **Common Use Today**: JavaScript programs included in web pages loaded into client browsers.

---
## 1.5.2	Openness

- **Definition**: Openness in computer systems refers to the ability to extend and reimplement the system in various ways.
- **Key Aspect**: Determined by the degree to which new resource-sharing services can be added and used by different client programs.
- **Publication of Interfaces**: Essential for openness; involves making the specification and documentation of key software interfaces available to developers.
- **Standardisation**: Similar to standardization but often bypasses official procedures due to their complexity and slow pace.
- **Challenges**: Designers must manage the complexity of distributed systems with components from different engineers.
- **RFCs**: Internet protocols are documented in ‘Requests For Comments’ (RFCs), which began in the early 1980s and include specifications and discussions.
- **W3C**: The World Wide Web Consortium also publishes standards related to web technologies.
- **Open Distributed Systems**:
    - Support resource sharing and are extensible.
    - Can be extended at both hardware and software levels.
    - Independent of individual vendors.

---
## 1.5.3 Security

- **Importance**: Security is crucial for information resources in distributed systems due to their high value.
- **Components**:
    - **Confidentiality**: Protection against unauthorised disclosure.
    - **Integrity**: Protection against alteration or corruption.
    - **Availability**: Protection against interference with access.
- **Internet and Intranet Risks**:
    - Free access to resources poses security risks.
    - Firewalls can restrict traffic but do not ensure appropriate resource use within an intranet or the Internet.
- **Client-Server Communication**:
    - Sensitive information is sent over networks.
    - Examples include doctors accessing patient data and users sending credit card numbers.
- **Challenges**:
    - **Message Security**: Ensuring the confidentiality and integrity of messages.
    - **User Identity Verification**: Confirming the identity of users or agents.
    - **Encryption**: Widely used to address these challenges.
- **Unresolved Security Issues**:
    - **Denial of Service Attacks**: Disrupting services by overwhelming them with requests.
    - **Security of Mobile Code**: Risks associated with running executable programs from untrusted sources.

---
## 1.5.4	Scalability

- **Definition**: A system is scalable if it remains effective with a significant increase in resources and users.
- **Growth**: The number of computers and web servers has dramatically increased, with notable growth in fixed and mobile personal computing.
- **Challenges**:
    - **Controlling Cost of Physical Resources**: Systems should extend at reasonable costs to meet growing demands (e.g., adding servers to avoid bottlenecks).
    - **Controlling Performance Loss**: Hierarchical structures scale better than linear ones; performance loss should be minimal.
    - **Preventing Software Resource Exhaustion**: Example: Internet (IP) addresses running out, leading to the adoption of 128-bit addresses.
    - **Avoiding Performance Bottlenecks**: Decentralised algorithms prevent bottlenecks; caching and replication improve performance.
- **Ideal Scenario**: System and application software should not need changes with increased scale, though this is challenging.
- **Successful Techniques**: Include replicated data, caching, and multiple servers for concurrent tasks.
---
## 1.5.5	Failure Handling

- **Failures in Distributed Systems**: Partial failures where some components fail while others continue to function.
- **Techniques for Dealing with Failures**:
    - **Detecting Failures**:
        - Some failures can be detected (e.g., using checksums for corrupted data).
        - Difficult to detect some failures, such as a remote crashed server.
    - **Masking Failures**:
        - Retransmitting messages that fail to arrive.
        - Writing file data to a pair of disks to prevent data loss.
    - **Tolerating Failures**:
        - Clients designed to tolerate failures (e.g., web browsers informing users when a server is unreachable).
    - **Recovery from Failures**:
        - Designing software to recover or ‘roll back’ the state of permanent data after a crash.
    - **Redundancy**:
        - Using redundant components to tolerate failures (e.g., multiple routes between routers, replicated name tables in DNS, replicated databases).
- **Availability**:
    - Distributed systems provide high availability despite hardware faults.
    - Users can switch to another computer or start a server process on another computer if a component fails.
---
## 1.5.6	Concurrency

- **Resource Sharing**: Services and applications in distributed systems provide resources that can be accessed by multiple clients simultaneously.
- **Concurrency Issues**: Multiple clients accessing a shared resource at the same time can lead to conflicts and inconsistent results.
- **Concurrency Management**:
    - Resources are often encapsulated as objects with invocations executed in concurrent threads.
    - Operations on these objects must be synchronised to maintain data consistency.
- **Example**: In an auction, concurrent bids might get interleaved incorrectly without proper control.
- **Responsibility**: Objects representing shared resources must ensure correct operation in a concurrent environment.
- **Synchronisation Techniques**: Standard techniques like semaphores are used to synchronise operations and maintain consistency.
- **Programmer’s Role**: Programmers must adapt objects not originally intended for distributed systems to be safe in concurrent environments.
---
## 1.5.7	Transparency

- **Definition**: Concealment of the separation of components in a distributed system, making it appear as a whole.
- **Forms of Transparency**:
    1. **Access Transparency**: Local and remote resources accessed using identical operations.
    2. **Location Transparency**: Resources accessed without knowing their physical or network location.
    3. **Concurrency Transparency**: Multiple processes operate concurrently without interference.
    4. **Replication Transparency**: Multiple resource instances used without user knowledge.
    5. **Failure Transparency**: Concealment of faults, allowing task completion despite failures.
    6. **Mobility Transparency**: Movement of resources and clients without affecting operations.
    7. **Performance Transparency**: System reconfiguration to improve performance as loads vary.
    8. **Scaling Transparency**: System and applications expand in scale without structural changes.
- **Key Transparencies**: Access and location transparency, often referred to as network transparency.
- **Examples**:
    - **Access Transparency**: Graphical user interfaces with folders that are the same for local and remote files.
    - **Location Transparency**: URLs that identify web server domain names rather than Internet addresses.
    - **Failure Transparency**: Email delivery despite server or communication link failures.
    - **Mobility Transparency**: Mobile phones moving between cells without affecting call operations.
- **Transparency Benefits**: Hides irrelevant resources, allowing interchangeable hardware allocation.
---
## 1.5.8	Quality of Service (QoS)

- **Definition**: Beyond functionality, <strong>QoS</strong> refers to the non-functional properties affecting user experience, such as *reliability*, *security*, and *performance*.
- **Key Aspects**:
    1. **Reliability**: Ensuring consistent and dependable service.
    2. **Security**: Protecting data and resources.
    3. **Performance**: Initially defined by responsiveness and throughput, now focused on meeting timeliness guarantees.
- **Time-Critical Data**: Applications like multimedia require data to be processed or transferred at fixed rates (e.g., video streaming).
- **QoS Requirements**:
    - Systems must provide guaranteed computing and network resources to meet deadlines.
    - High-performance networks can deteriorate under heavy load, lacking guarantees.
- **Resource Management**: Critical resources must be reserved by applications needing <strong>QoS</strong>, with resource managers providing guarantees and rejecting unmet requests.

---
# Chapter 1.6:	The World Wide Web (Case Study)
---
- **Overview**: The World Wide Web is an evolving system for publishing and accessing resources and services across the Internet.
- **Origins**: Began at CERN in 1989 to exchange documents among physicists.
- **Key Feature**: Hypertext structure with documents containing links to other documents and resources.
- **Openness**:
    - Based on freely published communication and content standards.
    - Supports various browsers and web servers on multiple platforms.
    - Allows publishing and sharing of diverse resources, including new formats via helper applications and plug-ins.
- **Evolution**: Expanded from simple data resources to services like electronic purchasing.
- **Technological Components**:
    1. **HTML**: Specifies contents and layout of web pages.
    2. **URLs/URIs**: Identify documents and resources on the Web.
    3. **Client-Server Architecture**: Uses HTTP for interaction between browsers/clients and web servers.
- **User Experience**: Users can access and manage web servers globally, enhancing the web’s extensibility and functionality.
---
## HTML

- **Purpose**: Specifies the text and images on a web page, and their layout and formatting.
- **Structure**: Web pages contain headings, paragraphs, tables, images, and links to other resources.
- **Creation**:
    - Can be written by hand using a text editor.
    - More commonly created using ‘wysiwyg’ editors that generate HTML from a graphical layout.
- **Example HTML**
```
<IMG SRC="http://www.cdk5.net/WebExample/Images/earth.jpg">
<P>
Welcome to Earth! Visitors may also be interested in taking a look at the 
<A HREF="http://www.cdk5.net/WebExample/moon.html">Moon</A>.
</P>
```
- **File Storage**: HTML files are stored on web servers (e.g., earth.html on www.cdk5.net).
- **Browser Function**:
    - Retrieves and renders HTML content from web servers.
    - Interprets HTML to display formatted text and images.
    - Informs the browser of the content type based on file extensions like ‘.html’.
- **HTML Tags**: Enclosed in angle brackets (e.g., `<P>` for paragraphs).
- **Links**:
    - Specified using `<A HREF="URL">link text</A>`.
    - Browsers typically underline link text.
    - Clicking a link retrieves and displays the associated resource.
---
## URLs

- **Purpose**: Identify a resource on the Web, often referred to as Uniform Resource Identifiers (URIs).
- **Usage**: Browsers use URLs to access resources, either typed by users, clicked as links, or selected from bookmarks.
- **Structure**:
    - **Scheme**: Declares the type of URL (e.g., `http`, `mailto`, `ftp`).
    - **Scheme-Specific Identifier**: Identifies the resource.
- **HTTP URLs**: Most common, used to access resources via the HTTP protocol.
    - **Format**: `http://servername[:port][/pathName][?query][#fragment]`
    - **Components**:
        - **Server Name**: Domain Name System (DNS) name.
        - **Port**: Optional, default is 80.
        - **Path Name**: Optional, identifies the resource on the server.
        - **Query**: Optional, used for search queries.
        - **Fragment**: Optional, identifies a component within the resource.
- **Examples**:
    - `http://www.cdk5.net` (default page)
    - `http://www.w3.org/standards/faq.html#conformance` (fragment identifier)
    - `http://www.google.com/search?q=obama` (search query)
- **Publishing Resources**:
    - Place the file in a web server directory.
    - Construct the URL as `http://S/P` where `S` is the server name and `P` is the path.
    - Distribute the URL via links or email.
- **Content Management**: Tools like blogging software and content management systems simplify content creation and publishing
---
## HTTP

- **Definition**: The HyperText Transfer Protocol defines how browsers and clients interact with web servers.
- **Request-Reply Interactions**:
    - HTTP is a request-reply protocol.
    - Clients send a request with the URL of the resource.
    - Servers respond with the resource content or an error message (e.g., ‘404 Not Found’).
    - Common methods: `GET` (retrieve data) and `POST` (provide data).
- **Content Types**:
    - Browsers specify preferred content types in requests.
    - Servers include the content type in responses.
    - MIME types standardise content types (e.g., `text/html`, `image/GIF`, `application/zip`).
- **One Resource per Request**:
    - Each HTTP request specifies one resource.
    - Browsers issue multiple requests for pages with multiple resources (e.g., images).
    - Concurrent requests reduce overall delay.
- **Simple Access Control**:
    - Default: Any user with network access can access published resources.
    - Restricted access: Servers can issue challenges requiring users to prove access rights (e.g., passwords).
---
## Dynamic Pages

- **User Interaction**: Users often interact with services by filling out web forms, which are web pages with input widgets like text fields and check boxes.
- **HTTP Requests**:
    - When a form is submitted, the browser sends an HTTP request to the server with the user’s input.
    - The URL designates a program on the server, not a file.
    - Input can be sent as a query component (GET method) or additional data (POST method).
- **Example**:
    - URL: `http://www.google.com/search?q=obama`
    - Invokes a program called ‘search’ and returns HTML text with search results.
- **CGI Programs**:
    - Common Gateway Interface (CGI) programs generate content dynamically.
    - They parse client-provided arguments and produce the required content, often consulting or updating a database.
- **Downloaded Code**:
    - JavaScript: Enhances user interaction by providing immediate feedback and updating parts of a web page dynamically.
    - AJAX: Asynchronous JavaScript and XML, used for asynchronous updates without reloading the entire page.
    - Applets: Java applications downloaded and run by the browser, providing customised user interfaces and network access.
---
## Web Services

- **Programmatic Access**: Programs other than browsers can access web resources.
- **Limitations of HTML**:
    - Not suitable for programmatic interoperation.
    - Limited to static structures and presentation-bound data.
- **XML**:
    - Extensible Markup Language designed for structured, application-specific data.
    - Self-describing and portable between applications.
    - Used in HTTP (via POST and GET) and AJAX for data exchange.
- **Service-Specific Operations**:
    - Example: Ordering a book or checking order status on amazon.com.
    - HTTP methods: GET, POST, PUT, DELETE.
- **REST Architecture**:
    - REpresentational State Transfer.
    - Extensible approach where every resource has a URL and responds to standard operations.
    - Described in detail in Chapter 9, including the web services framework for specifying service-specific operations.
---
## Discussion of the Web

- **Success Factors**:
    - Ease of publishing resources by individuals and organisations.
    - Hypertext structure suitable for organising various types of information.
    - Open system architecture with simple, widely published standards.
- **Design Problems**:
    - **Dangling Links**: Links to deleted or moved resources causing user frustration.
    - **Lost in Hyperspace**: Users getting confused by following disparate links from various sources.
- **Search Engines**:
    - Popular but imperfect in delivering specific user intents.
    - ***Resource Description Framework (RDF)*** aims to improve searches with standard vocabularies, syntax, and semantics for metadata.
    - The semantic web uses linked metadata resources for more accurate searches.
- **Scalability Issues**:
    - Popular web servers may experience slow responses due to high traffic.
    - Solutions include caching in browsers and proxy servers, and load distribution across server clusters.

---
# Chapter 1.7:	Summary
---
- **Prevalence**: Distributed systems are ubiquitous, enabling global access to services via the Internet and local services via intranets.
- **Resource Sharing**: Main motivation for constructing distributed systems, managed by servers and accessed by clients (e.g., browsers for web servers).
- **Challenges**:
    - **Heterogeneity**: Constructed from diverse networks, OS, hardware, and languages; middleware and Internet protocols help manage differences.
    - **Openness**: Systems should be extensible; publishing interfaces is the first step, but integrating components from different programmers is challenging.
    - **Security**: Encryption protects shared resources and sensitive information; denial of service attacks remain an issue.
    - **Scalability**: Systems should scale efficiently; algorithms should avoid bottlenecks, and data should be structured hierarchically and replicated if frequently accessed.
    - **Failure Handling**: Components must handle independent failures appropriately.
    - **Concurrency**: Resources must be designed to handle concurrent requests safely.
    - **Transparency**: Certain aspects of distribution should be invisible to application programmers, who should focus on their application’s design.
    - **Quality of Service**: Guarantees regarding performance, security, and reliability are essential for service access.