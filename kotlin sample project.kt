import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max

/*
 * SINGLE-FILE BANKING MANAGEMENT SYSTEM
 * Kotlin/JVM console application.
 *
 * Features:
 * - Registration and login
 * - Accounts, deposits, withdrawals, transfers
 * - Transaction history and statements
 * - PIN change and profile updates
 * - Admin reports
 * - File-based persistence
 *
 * Demo note: This is an educational sample, not production banking software.
 * Passwords/PINs and storage are simplified for demonstration purposes.
 */

private const val DATA_FILE = "bank-data.txt"
private const val ADMIN_USERNAME = "admin"
private const val ADMIN_PASSWORD = "admin123"
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class Customer(
    val id: String,
    var name: String,
    var username: String,
    var password: String,
    var phone: String,
    var email: String,
    var active: Boolean = true
)

data class BankAccount(
    val number: String,
    val customerId: String,
    var type: String,
    var balance: Long = 0L, // stored in paise to avoid floating-point money errors
    var active: Boolean = true
)

data class BankTransaction(
    val id: String,
    val timestamp: String,
    val type: String,
    val accountNumber: String,
    val amount: Long,
    val balanceAfter: Long,
    val description: String,
    val relatedAccount: String = ""
)

class BankingSystem {
    private val customers = linkedMapOf<String, Customer>()
    private val accounts = linkedMapOf<String, BankAccount>()
    private val transactions = mutableListOf<BankTransaction>()
    private var currentCustomer: Customer? = null
    private var running = true

    init {
        loadData()
        if (customers.isEmpty()) seedDemoData()
    }

    fun run() {
        println()
        println("======================================================")
        println("             KOTLIN BANKING MANAGEMENT")
        println("======================================================")
        while (running) {
            if (currentCustomer == null) showGuestMenu() else showCustomerMenu()
        }
        saveData()
        println("Thank you for using Kotlin Banking Management.")
    }

    private fun showGuestMenu() {
        println()
        println("1. Register")
        println("2. Login")
        println("3. View demo credentials")
        println("4. Admin login")
        println("0. Exit")
        when (readInt("Choose: ")) {
            1 -> registerCustomer()
            2 -> login()
            3 -> showDemoCredentials()
            4 -> adminLogin()
            0 -> running = false
            else -> println("Invalid option.")
        }
    }

    private fun showCustomerMenu() {
        val customer = currentCustomer ?: return
        println()
        println("--------------- Welcome, ${customer.name} ---------------")
        println("1. Dashboard")
        println("2. My accounts")
        println("3. Open account")
        println("4. Deposit")
        println("5. Withdraw")
        println("6. Transfer")
        println("7. Transaction history")
        println("8. Account statement")
        println("9. Change password")
        println("10. Update profile")
        println("11. Close account")
        println("12. Logout")
        println("0. Exit")
        when (readInt("Choose: ")) {
            1 -> dashboard()
            2 -> listMyAccounts()
            3 -> openAccount()
            4 -> deposit()
            5 -> withdraw()
            6 -> transfer()
            7 -> transactionHistory()
            8 -> accountStatement()
            9 -> changePassword()
            10 -> updateProfile()
            11 -> closeAccount()
            12 -> logout()
            0 -> {
                saveData()
                running = false
            }
            else -> println("Invalid option.")
        }
    }

    private fun registerCustomer() {
        println("\n--- Customer Registration ---")
        val name = readNonEmpty("Full name: ")
        val username = readNonEmpty("Username: ").lowercase()
        if (customers.values.any { it.username.equals(username, true) }) {
            println("That username is already taken.")
            return
        }
        val password = readPassword("Password (minimum 6 characters): ")
        if (password.length < 6) {
            println("Password must have at least 6 characters.")
            return
        }
        val phone = readNonEmpty("Phone: ")
        val email = readNonEmpty("Email: ")
        val customer = Customer(
            id = "C" + UUID.randomUUID().toString().take(8).uppercase(),
            name = name,
            username = username,
            password = password,
            phone = phone,
            email = email
        )
        customers[customer.id] = customer
        currentCustomer = customer
        println("Registration successful. Your customer ID is ${customer.id}.")
        if (accounts.values.none { it.customerId == customer.id }) {
            createAccountFor(customer, "Savings")
        }
        saveData()
    }

    private fun login() {
        println("\n--- Customer Login ---")
        val username = readNonEmpty("Username: ").lowercase()
        val password = readPassword("Password: ")
        val customer = customers.values.firstOrNull {
            it.username.equals(username, true) && it.password == password && it.active
        }
        if (customer == null) {
            println("Invalid credentials or inactive account.")
            return
        }
        currentCustomer = customer
        println("Login successful.")
        dashboard()
    }

    private fun showDemoCredentials() {
        println("\nDemo customer credentials")
        println("Username: demo")
        println("Password: demo123")
        println("Admin username: admin")
        println("Admin password: admin123")
        println("Use these only for this educational sample.")
    }

    private fun adminLogin() {
        println("\n--- Admin Login ---")
        val username = readNonEmpty("Username: ")
        val password = readPassword("Password: ")
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            adminMenu()
        } else {
            println("Invalid admin credentials.")
        }
    }

    private fun adminMenu() {
        var adminRunning = true
        while (adminRunning) {
            println("\n================ ADMIN PANEL ================")
            println("1. System overview")
            println("2. List customers")
            println("3. List all accounts")
            println("4. Search customer")
            println("5. Search account")
            println("6. Recent transactions")
            println("7. Toggle customer status")
            println("8. Export report")
            println("0. Back")
            when (readInt("Choose: ")) {
                1 -> adminOverview()
                2 -> adminListCustomers()
                3 -> adminListAccounts()
                4 -> adminSearchCustomer()
                5 -> adminSearchAccount()
                6 -> adminRecentTransactions()
                7 -> adminToggleCustomer()
                8 -> exportReport()
                0 -> adminRunning = false
                else -> println("Invalid option.")
            }
        }
    }

    private fun dashboard() {
        val customer = currentCustomer ?: return
        val ownAccounts = accounts.values.filter { it.customerId == customer.id && it.active }
        println("\n---------------- Dashboard ----------------")
        println("Customer: ${customer.name}")
        println("Customer ID: ${customer.id}")
        println("Username: ${customer.username}")
        println("Active accounts: ${ownAccounts.size}")
        println("Total balance: ${money(ownAccounts.sumOf { it.balance })}")
        println("Recent activity:")
        val ownNumbers = ownAccounts.map { it.number }.toSet()
        val recent = transactions.filter {
            it.accountNumber in ownNumbers || it.relatedAccount in ownNumbers
        }.takeLast(5).reversed()
        if (recent.isEmpty()) println("  No transactions yet.")
        recent.forEach { printTransaction(it) }
        println("-------------------------------------------")
    }

    private fun listMyAccounts() {
        val customer = currentCustomer ?: return
        val own = accounts.values.filter { it.customerId == customer.id }
        println("\n--- My Accounts ---")
        if (own.isEmpty()) {
            println("No accounts found.")
            return
        }
        own.forEachIndexed { index, account ->
            println("${index + 1}. ${account.number} | ${account.type} | " +
                "${money(account.balance)} | ${if (account.active) "Active" else "Closed"}")
        }
    }

    private fun openAccount() {
        val customer = currentCustomer ?: return
        println("\nAccount type")
        println("1. Savings")
        println("2. Current")
        println("3. Student")
        val type = when (readInt("Choose: ")) {
            1 -> "Savings"
            2 -> "Current"
            3 -> "Student"
            else -> {
                println("Invalid account type.")
                return
            }
        }
        createAccountFor(customer, type)
        saveData()
    }

    private fun createAccountFor(customer: Customer, type: String) {
        val number = "10" + (10000000..99999999).random().toString()
        val account = BankAccount(number, customer.id, type)
        accounts[number] = account
        println("$type account created. Account number: $number")
    }

    private fun deposit() {
        val account = chooseOwnAccount("Deposit into which account?")
        if (account == null) return
        val amount = readAmount("Deposit amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        account.balance += amount
        addTransaction("DEPOSIT", account, amount, "Cash deposit")
        saveData()
        println("Deposit successful. New balance: ${money(account.balance)}")
    }

    private fun withdraw() {
        val account = chooseOwnAccount("Withdraw from which account?")
        if (account == null) return
        val amount = readAmount("Withdrawal amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        if (account.balance < amount) {
            println("Insufficient balance.")
            return
        }
        account.balance -= amount
        addTransaction("WITHDRAWAL", account, -amount, "Cash withdrawal")
        saveData()
        println("Withdrawal successful. New balance: ${money(account.balance)}")
    }

    private fun transfer() {
        println("\n--- Transfer Funds ---")
        val source = chooseOwnAccount("Transfer from which account?")
        if (source == null) return
        val destinationNumber = readNonEmpty("Destination account number: ")
        val destination = accounts[destinationNumber]
        if (destination == null || !destination.active) {
            println("Destination account not found or inactive.")
            return
        }
        if (destination.number == source.number) {
            println("Source and destination must be different.")
            return
        }
        val amount = readAmount("Transfer amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        if (source.balance < amount) {
            println("Insufficient balance.")
            return
        }
        val note = readLineOptional("Transfer note (optional): ")
        source.balance -= amount
        destination.balance += amount
        addTransaction("TRANSFER_OUT", source, -amount, note.ifBlank { "Funds transferred" }, destination.number)
        addTransaction("TRANSFER_IN", destination, amount, note.ifBlank { "Funds received" }, source.number)
        saveData()
        println("Transfer successful.")
        println("Transferred ${money(amount)} to ${destination.number}.")
        println("Remaining balance: ${money(source.balance)}")
    }

    private fun transactionHistory() {
        val customer = currentCustomer ?: return
        val ownNumbers = accounts.values.filter { it.customerId == customer.id }.map { it.number }.toSet()
        val history = transactions.filter {
            it.accountNumber in ownNumbers || it.relatedAccount in ownNumbers
        }
        println("\n--- Transaction History (${history.size}) ---")
        if (history.isEmpty()) {
            println("No transactions found.")
            return
        }
        history.takeLast(100).reversed().forEach { printTransaction(it) }
    }

    private fun accountStatement() {
        val account = chooseOwnAccount("Generate statement for which account?")
        if (account == null) return
        val history = transactions.filter {
            it.accountNumber == account.number
        }
        println("\n================ ACCOUNT STATEMENT ================")
        println("Account number : ${account.number}")
        println("Account type   : ${account.type}")
        println("Current balance: ${money(account.balance)}")
        println("Generated      : ${now()}")
        println("---------------------------------------------------")
        if (history.isEmpty()) println("No transactions.")
        history.forEach { printTransaction(it) }
        println("===================================================")
        val export = readLineOptional("Save statement to file? (y/n): ")
        if (export.equals("y", true)) {
            val file = File("statement-${account.number}.txt")
            file.writeText(buildString {
                appendLine("ACCOUNT STATEMENT")
                appendLine("Account: ${account.number}")
                appendLine("Type: ${account.type}")
                appendLine("Balance: ${money(account.balance)}")
                appendLine("Generated: ${now()}")
                history.forEach { appendLine(formatTransaction(it)) }
            })
            println("Statement saved to ${file.absolutePath}")
        }
    }

    private fun changePassword() {
        val customer = currentCustomer ?: return
        val old = readPassword("Current password: ")
        if (old != customer.password) {
            println("Current password is incorrect.")
            return
        }
        val newPassword = readPassword("New password: ")
        if (newPassword.length < 6) {
            println("Password must have at least 6 characters.")
            return
        }
        val confirm = readPassword("Confirm new password: ")
        if (newPassword != confirm) {
            println("Passwords do not match.")
            return
        }
        customer.password = newPassword
        saveData()
        println("Password updated.")
    }

    private fun updateProfile() {
        val customer = currentCustomer ?: return
        println("\nLeave a field blank to keep its current value.")
        val name = readLineOptional("Name [${customer.name}]: ")
        val phone = readLineOptional("Phone [${customer.phone}]: ")
        val email = readLineOptional("Email [${customer.email}]: ")
        if (name.isNotBlank()) customer.name = name
        if (phone.isNotBlank()) customer.phone = phone
        if (email.isNotBlank()) customer.email = email
        saveData()
        println("Profile updated.")
    }

    private fun closeAccount() {
        val account = chooseOwnAccount("Close which account?")
        if (account == null) return
        if (account.balance != 0L) {
            println("Account must have zero balance before closing.")
            return
        }
        val confirmation = readLineOptional("Type CLOSE to confirm: ")
        if (confirmation != "CLOSE") {
            println("Cancelled.")
            return
        }
        account.active = false
        saveData()
        println("Account closed.")
    }

    private fun logout() {
        currentCustomer = null
        println("Logged out.")
    }

    private fun chooseOwnAccount(prompt: String): BankAccount? {
        val customer = currentCustomer ?: return null
        val own = accounts.values.filter { it.customerId == customer.id && it.active }
        if (own.isEmpty()) {
            println("You have no active accounts.")
            return null
        }
        println("\n$prompt")
        own.forEachIndexed { i, account ->
            println("${i + 1}. ${account.number} (${account.type}) - ${money(account.balance)}")
        }
        val choice = readInt("Account number from list: ")
        if (choice !in 1..own.size) {
            println("Invalid selection.")
            return null
        }
        return own[choice - 1]
    }

    private fun addTransaction(
        type: String,
        account: BankAccount,
        amount: Long,
        description: String,
        relatedAccount: String = ""
    ) {
        transactions.add(
            BankTransaction(
                id = "T" + UUID.randomUUID().toString().take(10).uppercase(),
                timestamp = now(),
                type = type,
                accountNumber = account.number,
                amount = amount,
                balanceAfter = account.balance,
                description = description,
                relatedAccount = relatedAccount
            )
        )
    }

    private fun printTransaction(transaction: BankTransaction) {
        println(formatTransaction(transaction))
    }

    private fun formatTransaction(transaction: BankTransaction): String {
        val sign = if (transaction.amount >= 0) "+" else "-"
        return "${transaction.timestamp} | ${transaction.type.padEnd(12)} | " +
            "$sign${money(kotlin.math.abs(transaction.amount))} | " +
            "Balance ${money(transaction.balanceAfter)} | ${transaction.description}" +
            if (transaction.relatedAccount.isNotBlank()) " | Related: ${transaction.relatedAccount}" else ""
    }

    private fun adminOverview() {
        println("\n--- System Overview ---")
        println("Customers: ${customers.size}")
        println("Active customers: ${customers.values.count { it.active }}")
        println("Accounts: ${accounts.size}")
        println("Active accounts: ${accounts.values.count { it.active }}")
        println("Transactions: ${transactions.size}")
        println("Total deposits held: ${money(accounts.values.filter { it.active }.sumOf { it.balance })}")
        println("Deposits: ${transactions.count { it.type == "DEPOSIT" }}")
        println("Withdrawals: ${transactions.count { it.type == "WITHDRAWAL" }}")
        println("Transfers: ${transactions.count { it.type.startsWith("TRANSFER") } / 2}")
    }

    private fun adminListCustomers() {
        println("\n--- All Customers ---")
        if (customers.isEmpty()) println("No customers.")
        customers.values.forEach {
            println("${it.id} | ${it.name} | ${it.username} | ${it.phone} | " +
                "${it.email} | ${if (it.active) "Active" else "Disabled"}")
        }
    }

    private fun adminListAccounts() {
        println("\n--- All Accounts ---")
        if (accounts.isEmpty()) println("No accounts.")
        accounts.values.forEach {
            val owner = customers[it.customerId]?.name ?: "Unknown"
            println("${it.number} | $owner | ${it.type} | ${money(it.balance)} | " +
                "${if (it.active) "Active" else "Closed"}")
        }
    }

    private fun adminSearchCustomer() {
        val query = readNonEmpty("Search by name, username, ID, or phone: ")
        val results = customers.values.filter {
            it.name.contains(query, true) ||
                it.username.contains(query, true) ||
                it.id.contains(query, true) ||
                it.phone.contains(query, true)
        }
        if (results.isEmpty()) println("No customers matched.")
        results.forEach {
            println("${it.id} | ${it.name} | ${it.username} | ${it.phone} | ${it.email}")
        }
    }

    private fun adminSearchAccount() {
        val number = readNonEmpty("Account number: ")
        val account = accounts[number]
        if (account == null) {
            println("Account not found.")
            return
        }
        println("Account: ${account.number}")
        println("Owner: ${customers[account.customerId]?.name ?: "Unknown"}")
        println("Type: ${account.type}")
        println("Balance: ${money(account.balance)}")
        println("Status: ${if (account.active) "Active" else "Closed"}")
        transactions.filter { it.accountNumber == number }.forEach { printTransaction(it) }
    }

    private fun adminRecentTransactions() {
        println("\n--- Recent Transactions ---")
        transactions.takeLast(50).reversed().forEach { printTransaction(it) }
    }

    private fun adminToggleCustomer() {
        val id = readNonEmpty("Customer ID: ")
        val customer = customers[id]
        if (customer == null) {
            println("Customer not found.")
            return
        }
        customer.active = !customer.active
        saveData()
        println("${customer.name} is now ${if (customer.active) "active" else "disabled"}.")
    }

    private fun exportReport() {
        val file = File("bank-report.txt")
        file.writeText(buildString {
            appendLine("BANK SYSTEM REPORT")
            appendLine("Generated: ${now()}")
            appendLine("Customers: ${customers.size}")
            appendLine("Accounts: ${accounts.size}")
            appendLine("Transactions: ${transactions.size}")
            appendLine("Total balances: ${money(accounts.values.sumOf { it.balance })}")
            appendLine()
            appendLine("CUSTOMERS")
            customers.values.forEach { appendLine("${it.id},${it.name},${it.username},${it.active}") }
            appendLine()
            appendLine("ACCOUNTS")
            accounts.values.forEach { appendLine("${it.number},${it.customerId},${it.type},${money(it.balance)},${it.active}") }
            appendLine()
            appendLine("TRANSACTIONS")
            transactions.forEach { appendLine(formatTransaction(it)) }
        })
        println("Report exported to ${file.absolutePath}")
    }

    private fun seedDemoData() {
        val demo = Customer(
            id = "C-DEMO",
            name = "Demo Customer",
            username = "demo",
            password = "demo123",
            phone = "9000000000",
            email = "demo@example.com"
        )
        val friend = Customer(
            id = "C-FRIEND",
            name = "Sample Recipient",
            username = "recipient",
            password = "recipient123",
            phone = "9111111111",
            email = "recipient@example.com"
        )
        customers[demo.id] = demo
        customers[friend.id] = friend
        accounts["1000000001"] = BankAccount("1000000001", demo.id, "Savings", 2500000)
        accounts["1000000002"] = BankAccount("1000000002", demo.id, "Student", 500000)
        accounts["1000000003"] = BankAccount("1000000003", friend.id, "Savings", 1000000)
        println("Demo data initialized.")
    }

    private fun saveData() {
        val file = File(DATA_FILE)
        file.writeText(buildString {
            appendLine("KOTLIN_BANK_V1")
            customers.values.forEach {
                appendLine(listOf("C", enc(it.id), enc(it.name), enc(it.username), enc(it.password),
                    enc(it.phone), enc(it.email), it.active).joinToString("|"))
            }
            accounts.values.forEach {
                appendLine(listOf("A", enc(it.number), enc(it.customerId), enc(it.type),
                    it.balance, it.active).joinToString("|"))
            }
            transactions.forEach {
                appendLine(listOf("T", enc(it.id), enc(it.timestamp), enc(it.type), enc(it.accountNumber),
                    it.amount, it.balanceAfter, enc(it.description), enc(it.relatedAccount)).joinToString("|"))
            }
        })
    }

    private fun loadData() {
        val file = File(DATA_FILE)
        if (!file.exists()) return
        try {
            val lines = file.readLines()
            if (lines.firstOrNull() != "KOTLIN_BANK_V1") return
            for (line in lines.drop(1)) {
                val p = line.split("|")
                when (p.firstOrNull()) {
                    "C" -> if (p.size >= 8) {
                        val c = Customer(dec(p[1]), dec(p[2]), dec(p[3]), dec(p[4]),
                            dec(p[5]), dec(p[6]), p[7].toBoolean())
                        customers[c.id] = c
                    }
                    "A" -> if (p.size >= 6) {
                        val a = BankAccount(dec(p[1]), dec(p[2]), dec(p[3]),
                            p[4].toLongOrNull() ?: 0L, p[5].toBoolean())
                        accounts[a.number] = a
                    }
                    "T" -> if (p.size >= 9) {
                        transactions.add(BankTransaction(
                            dec(p[1]), dec(p[2]), dec(p[3]), dec(p[4]),
                            p[5].toLongOrNull() ?: 0L, p[6].toLongOrNull() ?: 0L,
                            dec(p[7]), dec(p[8])
                        ))
                    }
                }
            }
        } catch (exception: Exception) {
            println("Could not read saved data: ${exception.message}")
        }
    }

    private fun enc(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun dec(value: String): String =
        String(java.util.Base64.getDecoder().decode(value), Charsets.UTF_8)

    private fun readInt(prompt: String): Int {
        print(prompt)
        return readLine()?.trim()?.toIntOrNull() ?: -1
    }

    private fun readAmount(prompt: String): Long {
        print(prompt)
        val input = readLine()?.trim()?.replace(",", "") ?: return -1L
        return try {
            val amount = input.toBigDecimal()
            amount.multiply(java.math.BigDecimal(100)).longValueExact()
        } catch (_: Exception) {
            -1L
        }
    }

    private fun readNonEmpty(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readLine()?.trim().orEmpty()
            if (value.isNotEmpty()) return value
            println("This field cannot be empty.")
        }
    }

    private fun readPassword(prompt: String): String {
        print(prompt)
        return readLine()?.trim().orEmpty()
    }

    private fun readLineOptional(prompt: String): String {
        print(prompt)
        return readLine()?.trim().orEmpty()
    }

    private fun money(paise: Long): String {
        val rupees = paise / 100
        val cents = max(0L, kotlin.math.abs(paise % 100))
        val sign = if (paise < 0) "-" else ""
        return "₹$sign$rupees.%02d".format(cents)
    }

    private fun now(): String = LocalDateTime.now().format(dateFormat)
}
nsider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 042: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 043: Null safety



fun main() {
    BankingSystem().run()
}

