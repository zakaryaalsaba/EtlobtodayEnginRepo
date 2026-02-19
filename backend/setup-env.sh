#!/bin/bash

# Helper script to set MySQL password in .env file

echo "Please enter your MySQL root password:"
read -s MYSQL_PASS

# Update .env file with password
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/MYSQL_PASSWORD=.*/MYSQL_PASSWORD=$MYSQL_PASS/" .env
else
    # Linux
    sed -i "s/MYSQL_PASSWORD=.*/MYSQL_PASSWORD=$MYSQL_PASS/" .env
fi

echo "✓ MySQL password updated in .env file"
echo ""
echo "Now you can run: npm start"

