# 📋 Task Manager CLI

A simple, elegant command-line task manager built with Python.

## Features

- ✅ Add tasks with priority levels (high, medium, low)
- 📝 List all tasks or filter by completion status
- ✅ Mark tasks as completed
- 🗑️ Delete tasks
- 📊 View task statistics
- 💾 Persistent storage (JSON file)

## Installation

1. Make sure you have Python 3.6+ installed
2. Clone or download this project
3. No additional dependencies required!

## Usage

Run the task manager:
```bash
python task_manager.py
```

### Commands

- `add "Buy groceries" high` - Add a new task with high priority
- `list` - Show pending tasks
- `list --all` - Show all tasks including completed ones
- `complete 1` - Mark task with ID 1 as completed
- `delete 2` - Delete task with ID 2
- `stats` - Show task statistics
- `help` - Show help information
- `quit` - Exit the program

## Example Session
