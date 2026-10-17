# FinCore - Banking Management System

A desktop banking application built with Java and Swing that lets customers
manage their accounts, move money, and download account statements as PDFs.

## Features
- User login and session handling
- Account dashboard showing name, account number, and live balance
- Deposit, withdraw, and transfer funds between accounts
- Full transaction history with date, type, amount, and balance after each transaction
- Downloadable PDF account statement (generated with OpenPDF)
- Input validation and BigDecimal-based money handling for accurate calculations

## Tech Stack
- **Language:** Java 17
- **UI:** Java Swing
- **PDF generation:** OpenPDF
- **Database:** MySQL via JDBC 
- **IDE:** IntelliJ IDEA

## Project Structure
src/Banking_System/
 - BankService.java    Business logic (accounts, transactions)
 - StatementPdf.java   PDF statement generator
 - Ui/                 Swing screens (login, dashboard)
lib/
 - openpdf-1.3.30.jar  PDF library
 - 
## Future Improvements
- Role-based admin panel
- Password hashing and stronger authentication
- Email statements to the customer
