## Architecture

...

---

## Postman Collection – End-to-End Testing

This repository includes a Postman collection used to demonstrate the backend capabilities of the Task Scheduler application in an end-to-end flow. While the focus is on backend architecture (authentication, request validation, external API integration, and state handling), the flow reflects how the API would be consumed in a real full-stack application.

### Setup

Import the collection into Postman:
* Click the **Import** button on the top of the Workspace panel.
* Drag and drop the _./postman/Task Scheduler - E2E testing.postman_collection.json_ file in the file selection window.

**Collection variables**

* `baseUrl` → URL matching where the BFF application is running (`http://localhost:8083`). **Doesn't need to be modified**.
* `email` → Email used to register/login. Preferably a real email to receive task notifications, but a dummy email also works for testing.
* `password` → Password used to register/login. Any password for testing.

---

### Automatic Variable Handling

Some variables are created and managed automatically during execution:

* `authToken`

  * Created automatically after successful login
  * Stored at collection level
  * Automatically injected into protected endpoints
  * Cleared if login fails

* Address variables (`mainAddressStreet`, `mainAddressCity`, etc.)

  * Created automatically after calling the address lookup endpoint
  * Required before creating a user

---

### Pre-request Scripts

The collection includes a collection-level **Pre-request Script** that:

* Defines public routes (e.g. login, register, address lookup)
* Treats all other routes as protected by default
* Automatically injects the `Authorization` header when required
* Blocks execution if authentication is missing

Some requests also include small guards to ensure required variables (like address data) exist before execution.

---

### Suggested Execution Order

1. Get address by CEP
2. Create user
3. Login
4. Execute protected endpoints (add address/phone, create task, etc.)
