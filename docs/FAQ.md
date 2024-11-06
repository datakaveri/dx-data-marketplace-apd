<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Frequently Asked Questions (FAQs)

1. How do I request for a new feature to be added or change in an existing feature?
- Please create an issue [here](https://github.com/datakaveri/iudx-data-marketplace-apd/issues)
2. What do we do when there is any error during flyway migration?
- We could run this command `mvn flyway:repair` and do the flyway migration again
-If the error persists, it needs to be resolved manually and a backup of the database could be taken from postgres if the table needs to be changed

3. “Product creation is forbidden” - even if the provider has created a linked account and an account on Razorpay
- This error occurs when the account that is created at Razorpay is not activated yet. The Razorpay account could be activated by provider visiting their merchant dashboard to complete their KYC, account information etc.,
- Please refer : [Creating a merchant Razorpay account](https://razorpay.com/docs/payments/create-account/)

4. Can the constraints given while creating the policy for a resource be different ?
- Yes, the constraints given to access the resource is defined by DX Resource server and provider could follow the same json structure to add different type of user specific constraints

5. Are there any access restrictions for delegates of the provider or consumer?
- No, delegates can access all the specific provider or consumer related APIs. Please refer [users and roles](https://github.com/datakaveri/iudx-data-marketplace-apd/blob/main/docs/Explanation.md)

6. Can the products, product variants be created on resource group level?
- No creation of product, product variants, policies are restricted to be created on resource item level

7. What is the currency in which transaction is currently done?
- Indian Rupee (INR)

8. Are there are any pre-requisites for the resources that are being published as product onto DX DMP APD ?
- Yes, when the resources are being created on DX Catalogue Server, the resource need to have specific DX DMP APD URL as it's APD URL

9. How can I administer all the payments done to my account when the consumer purchases my product variant?
- The purchase information could be fetched from list purchases API. The provider could also visit their merchant dashboard to manage their transaction, initiate refunds etc.,

10. I have purchased the same resource that was present in different product variants. How can I get an access token for the resource with a specific capability or constraint?
- The consumers can get access token from DX Auth Server for a specific resource with a specific constraint by providing `context` object with `orderId` in the request body
  - Reference to [requesting resource with context object in body](https://authorization.iudx.org.in/apis#tag/Token-APIs/operation/post-auth-v1-token) 
  - To get order ID consumer could use list purchase API by specifying the resourceId. [Reference to the API](https://redocly.github.io/redoc/?url=https://raw.githubusercontent.com/datakaveri/iudx-data-marketplace-apd/refs/heads/main/docs/openapi.yaml#tag/Consumer-List-APIs/paths/~1consumer~1list~1purchases/get)

11. "Merchant account has been locked by the admin" while creating, updating linked account
- The account might be locked by the Razorpay admin and might be under review. Please contact [Razorpay support](https://razorpay.com/support/#request) to get further help to resolve the issue. 