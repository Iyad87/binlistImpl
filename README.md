# binlistImpl
Integration with BinList API

A Use Case Implementation for External API Integration
Introduction

I did a simple task of integrating with the Bin List API and returning an expected client response different from the response gotten from the Bin List API. I will like to point out some of the basics in designing and building a system for such a task.
The BinList API is a public API that is used to fetch payment card details. It can be accessed via here Bin List API. It exposes an endpoint where you provide the first eight (8) digits of the card number and it returns the card details. For example below is a request and corresponding response made to the API:

  GET https://lookup.binlist.net/45717260
    {
        "number": {
        "length": 16,
        "luhn": true
        },
        "scheme": "visa",
        "type": "debit",
        "brand": "Visa/Dankort",
        "prepaid": false,
        "country": {
        "numeric": "208",
        "alpha2": "DK",
        "name": "Denmark",
        "emoji": "🇩🇰",
        "currency": "DKK",
        "latitude": 56,
        "longitude": 10
        },
        "bank": {
        "name": "Djurslands Bank",
        "url": "www.djurslandsbank.dk"
        }
    }
I won't be doing much of code screen shots but you can access every class and method names and signatures highlighted boldly in my github repo.

I will place emphasis on the following areas for sake of time and space.

Design pattern implementations (DTO, Service Facade, Repository, Cacheless Cow)

Performance(Caching)

Testing(Unit and Integration)

Tools/Platform used
Spring Boot 2.1.2 RELEASE

Maven 3.5.3

Java 8

Mysql 5.7.25

Ubuntu 16.04

Design Pattern Implementation
Because I was doing an integration which required data transfer and operations, these design patterns stood out immediately as to the strategies to be employed

Data Transfer Object (DTO) pattern

Service Facade pattern

Repository pattern

Cacheless Cow anti pattern

The Data Transfer Object(DTO) pattern is used when you want to transfer data across services or domains in a well structured format. It also allows for data structure transformation between domains. It's expected that the DTOs should be serializable. So while the BinListResponse class encapsulated the data retrieved from the BinList API (whose url I have configured in the application.yml), the CardDetailDto class contained the transformed data format that will be understood by the client. The transformation is done in the convertToCardDetailDto(BinListResponse binListResponse) method of the CardDetailService class. An advantage of this approach is that either the BinList API response or the client's expectation can be changed internally without affecting either. Also this will prevent breakage of the BinList API interface should the data format decide to change. This approach also makes use of Encapsulation in Object Oriented Programming (OOP) paradigm. All DTOs employed in the application can be located in the com.projects.binlist.dto.responses package.

The Service Facade or simply Facade is a strategy allows you to encapsulate and hide the complexities of carrying out an operation thereby exposing a simple interface to the client or caller of the method. This interface with this characteristics is sometimes referred to as being coarse. For example the getCardRequestLogsCountGroupedByCard(Pageable pageable) in the CardDetailService class does a bunch of things before returning the data to the client, i.e check the cache store, call the repository and transform the data retrieved to suit the client's expectation. The advantage of this is that it exposes a neat and simple interface and encapsulates related and seemingly linked operations that depend on one another as a single unit of operation thereby maintaining the integrity of the system's operation and output to the client. The client also has an advantage in that these operations are clearly transparent and they do not need to know the implementation of retrieving the data. (I like to keep my controllers thin and simple).

The Repository pattern as we all know allows for easy interchange of the Data Access Layer implementation. It's usually made up of interfaces implemented by the Data Access Object (DAO) (DAO which is another design pattern) classes. Spring provides different Repository interfaces which can be extended and implemented depending on our data storage engine (Mysql, MongoDB etc). As you can see, my repository domain is just a bunch of generics interfaces with the entity it's acting on as the type parameter as seen in the CardDetailRequestLogRepository class. The Repository pattern allows for loose coupling and easy testing as we will see in a bit. Also I have the advantage of writing customized queries and I can easily change them depending on either the data storage engine or the ORM implementation being used.

I will be talking about the Cacheless Cow anti pattern in the next section.

Performance(Caching)
Remember when I said in the last section concerning the Service Facade about how it exposes a simple interface to call by encapsulating a bunch of operations as a single unit of operation to produce the expected data output? Well this comes at a cost in terms of time it will take to produce that output for every call. However we can eliminate this cost by storing the data output and retrieving it whenever we need that output without going through the steps of reprocessing it provided we have some sort of identifier or key that we can do a lookup for that maps to that particular data we want to retrieve. This is similar to Dynamic Programming when we implement Recursion Algorithm by storing a previously computed value with a key in a data structure (Array or Map) and retrieving it without having to make a recursive call for that value so as to reduce time complexities.

In our application, we used a cache implementation to store that value so we don't have to make expensive database calls and data transformations for every service call. Caching in this context simply means

Get me that data as is if you have it without reprocessing or recomputing it.

This is how we handle what we normally term the Cacheless Cow anti pattern I referred to earlier.

To implement this in our code, we configure a Spring Cache manager bean in one of our configuration classes, the CacheConfig class using the ConcurrentMapCacheManager implementation (we could have also easily used the EhCache implementation as well). Then we annotate the service method getCardRequestLogsCountGroupedByCard(Pageable pageable) in our CardDetailService class with @Cacheable providing the necessary values (the value represents the cache name in our configuration file while the key is the key naming strategy we are using). Here in the key naming strategy, we concatenate the method name with the pageNumber attribute value of the Pageable parameter we are sending to generate a key and store the returned data with that key. So the first request will go through the processing steps to retrieve the data and then store it in the cache using the generated key. This took about 276ms for a 20 record table. Subsequent requests will ignore the processing steps and just fetch the stored data from the cache taking about 5-10ms (Wow!). The reprocessing of the data will occur only when you send a different pageNumber attribute value of the Pageable parameter that was not previously sent. A warning here however is to find a way to clear the data from the cache if the data from the backend has changed to prevent stale data being returned. Hence when the logs are updated in the database in the logCardDetailRequest(String iinStart) method, the @CacheEvict annotation on it clears the cache so it will force the method call to getCardRequestLogsCountGroupedByCard(Pageable pageable) to actually fetch the data from the database as it will no longer be in the cache. I will show you in a bit how we can verify this in our test class.

Testing
Writing tests can never be over emphasized during application development. Writing tests against your functionalities guarantees integrity and raises red flags if any new code change breaks your application. Before I continue, I would like to ask a rhetorical question here:

Why do you write tests or why are you writing that test?

For those of us who thought

I want to verify that my functionalities or endpoints works properly.

, chances are that you may be inclined to write your tests around what we refer to as Integration (or Functional) tests. For others who thought

I want to verify that my method works properly.

,chances are that you will end up writing Unit tests. There is nothing wrong in having both tests in your test suite but you need to consider the following when you decide which one to implement or even implement both.

Integration tests are normally environment dependent such that one that was written and passed successfully on your local machine might fail on another machine due to different data states in the different database instances or unavailability of a resource dependency due to network failure. Such a scenario can be seen when you try to run the CardDetailControllerITTest or the CardDetailControllerTest classes. These test classes expect the following in any environment it's running to be true

That the Bin List API is accessible
That the database is available and accessible
That the datasets in the databases of any of the environment must be the same.
Despite these constraints, a clear advantage of integration test is that you get to actually see how your dependencies behave in a real deployed scenario in the production environment. For example, the CacheIntegrationTest test class asserts that the responses gotten in both service calls are the same (i.e the cached response created in the first call is returned in the second call). It also verifies that the repository layer was only accessed once after both calls indicating that the second call didn't go to the database but retrieved the object from cache.

On the other hand, Unit tests are independent of the data states of the environment and will yield the same results regardless of the environment it's being run in. The reason for this is that the dependencies, resources, outputs and data are mocked, hence are not required to be available or accessible. Only pure logic and component method behaviours are tested here. For example, in the CardDetailServiceTest class, the RestTemplate dependency which is used to access the Bin List API is mocked along with the response. (This is like using the Guzzle MockHandler in the PHP or nock in the Node worlds). Also you will observe that logCardDetailRequest(String iinStart) method behaviour of the CardDetailService class is equally mocked by instructing it to do nothing as this interacts with the database (Thanks to the @Spy annotation). In the end, all we are testing is our DTO pattern implementation (i.e convertToCardDetailDto(BinListResponse binListResponse) method), to ensure the mocked BinListResponse object state that will be gotten from the Bin List API is transformed to the appropriate mocked CardDetailDto object state and returned to the client or method caller.

Can you guess the downside of this approach of testing? Yeah that's right, it doesn't simulate the true behaviours of your dependencies when they are deployed in production environment. Choosing which tests(unit or integration) to implement in your code is a function of the coding review requirements, use case or business requirements and the strategy employed in the deployment pipeline.

I have left out other features such as Security, Exception handling and Logging in this task as they were not necessarily a fundamental requirement. I hope I can get to share my insights on this as well sometime soon. This approach outlined in executing this task is by no means cast in stone as I welcome comments and other better ways or approaches in implementing such a task. 
