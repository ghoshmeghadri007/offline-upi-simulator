# Offline UPI Simulator

## Overview
A Spring Boot based simulator for exploring offline digital payments
using a mesh-style communication and deferred settlement approach.

## Features
- User and wallet management
- Offline spending limit
- Offline payment creation
- Mesh/gossip simulation
- Transaction encryption
- Deferred settlement
- Idempotent transaction processing
- Transaction ledger
- Demo dashboard

## Tech Stack
- Java 21
- Spring Boot
- Spring Data JPA
- MySQL
- Maven
- HTML/CSS/JavaScript

## How it Works

Sender  
↓  
Offline Payment  
↓  
Mesh Packet  
↓  
Gossip / Relay  
↓  
Bridge Upload  
↓  
Backend Settlement  
↓  
Transaction Completed

## Running the project
1. Create the MySQL database
2. Configure database credentials
3. Run the Spring Boot application
4. Open the dashboard

## Future Improvements
- React frontend
- More realistic device-to-device mesh simulation
- Authentication
- Better transaction visualization