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

//==============================================================================
// EXTENDED PROJECT DOCUMENTATION
//==============================================================================
//
// PROJECT OVERVIEW
// ----------------
// This application demonstrates object-oriented programming in Kotlin.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// It is intended for learning and local experimentation.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// The app runs in a terminal and uses a text file for persistence.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// All business operations are performed in memory during the current run.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Data is written to the local working directory when changes are made.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// SECURITY NOTES
// --------------
// This is not a production banking platform.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Passwords are stored in a reversible educational file format.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Base64 encoding is not encryption and does not protect confidential data.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// A real application should hash passwords with a dedicated password hashing algorithm.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// A real system must enforce authorization at every service boundary.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Real banking systems need audit logs, concurrency control, and transaction guarantees.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Never use demo credentials for a real service.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// MONEY MODEL
// -----------
// Money is represented internally as integer paise.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Integer minor units avoid common floating-point rounding errors.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// User input is parsed as a decimal amount and converted to paise.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Display formatting converts paise into rupees and paise.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// A production system should define explicit currency and rounding policies.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// RUNNING THE PROJECT
// -------------------
// Save this source as Main.kt.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Install a compatible Kotlin compiler and Java runtime.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Compile with: kotlinc Main.kt -include-runtime -d StudentBank.jar.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Run with: java -jar StudentBank.jar.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// The program creates bank-data.txt in the current working directory.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Keep the data file in a safe location if you want to preserve the demo state.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// FEATURE GUIDE
// -------------
// Registration creates a customer record and a default savings account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Login checks the demo credentials stored in the local data model.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// The dashboard displays active accounts and recent activity.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Deposits increase an account balance and add a transaction record.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Withdrawals check that the requested amount is positive and affordable.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Transfers debit one account and credit another account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Transaction history shows recent operations associated with the signed-in customer.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Statements can be printed and saved to a text file.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Profile editing allows the customer to update their contact information.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Account closure is allowed only when the balance is zero.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Admin tools show system totals and searchable records.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Reports can be exported for local inspection.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// TESTING CHECKLIST
// -----------------
// Register a new customer with a unique username.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Try registering a second customer with the same username.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Log in with correct credentials.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Try logging in with an incorrect password.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Deposit a positive amount.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to deposit zero.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to deposit malformed input.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Withdraw less than the available balance.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to withdraw more than the available balance.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Transfer money to another active account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to transfer to the same account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to transfer more than the source balance.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Review the transaction history after each operation.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Generate and save an account statement.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Change the password and test login with the new password.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Update the profile and confirm the changes remain after restart.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Close a zero-balance account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Attempt to close a non-zero-balance account.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Sign in to the admin panel and export a report.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
// Restart the program and check that saved records are loaded.
//
// Learning prompt: explain this point in your own words.
// Extension idea: write a small test or improve the design.
// Review question: what failure cases should the program handle?
//
//
// STUDY NOTE 001: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 002: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 003: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 004: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 005: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 006: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 007: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 008: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 009: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 010: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 011: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 012: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 013: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 014: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 015: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 016: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 017: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 018: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 019: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 020: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 021: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 022: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 023: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 024: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 025: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 026: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 027: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 028: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 029: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 030: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 031: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 032: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 033: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 034: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 035: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 036: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 037: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 038: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 039: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 040: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 041: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
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
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 044: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 045: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 046: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 047: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 048: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 049: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 050: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 051: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 052: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 053: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 054: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 055: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 056: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 057: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 058: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 059: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 060: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 061: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 062: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 063: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 064: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 065: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 066: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 067: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 068: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 069: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 070: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 071: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 072: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 073: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 074: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 075: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 076: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 077: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 078: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 079: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 080: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 081: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 082: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 083: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 084: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 085: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 086: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 087: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 088: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 089: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 090: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 091: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 092: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 093: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 094: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 095: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 096: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 097: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 098: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 099: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 100: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 101: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 102: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 103: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 104: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 105: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 106: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 107: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 108: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 109: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 110: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 111: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 112: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 113: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 114: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 115: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 116: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 117: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 118: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 119: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 120: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 121: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 122: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 123: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 124: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 125: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 126: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 127: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 128: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 129: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 130: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 131: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 132: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 133: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 134: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 135: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 136: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 137: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 138: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 139: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 140: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 141: Kotlin data classes
// Data classes generate equals, hashCode, toString, component functions, and copy.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 142: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 143: Null safety
// Nullable references require explicit handling before accessing their values.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 144: Functions
// Small functions separate user interaction from account operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 145: Validation
// Validate inputs before mutating balances or writing transactions.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 146: Persistence
// A local file is simple for a demo but lacks database guarantees.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 147: Testing
// Test both successful operations and rejected operations.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 148: Refactoring
// A production codebase should separate UI, services, repositories, and models.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 149: Error handling
// File operations and parsing should handle malformed input safely.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 150: Auditability
// Transactions record timestamps, operation types, amounts, and resulting balances.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//

fun main() {
    BankingSystem().run()
}

