#!/usr/bin/env python3
"""
Personal Task Manager CLI
A simple command-line tool to manage your daily tasks.
"""

import json
import os
import sys
from datetime import datetime, timedelta
from typing import List, Dict, Any


class TaskManager:
    def __init__(self, data_file: str = "tasks.json"):
        self.data_file = data_file
        self.tasks = self.load_tasks()
    
    def load_tasks(self) -> List[Dict[str, Any]]:
        """Load tasks from JSON file."""
        if os.path.exists(self.data_file):
            try:
                with open(self.data_file, 'r') as f:
                    return json.load(f)
            except (json.JSONDecodeError, IOError):
                print(f"Error loading tasks from {self.data_file}")
                return []
        return []
    
    def save_tasks(self) -> bool:
        """Save tasks to JSON file."""
        try:
            with open(self.data_file, 'w') as f:
                json.dump(self.tasks, f, indent=2)
            return True
        except IOError:
            print(f"Error saving tasks to {self.data_file}")
            return False
    
    def add_task(self, description: str, priority: str = "medium", due_date: str = None) -> bool:
        """Add a new task with optional due date."""
        if not description.strip():
            print("Error: Task description cannot be empty.")
            return False
        
        # Parse due date if provided
        parsed_due_date = None
        if due_date:
            try:
                # Try different date formats
                for date_format in ["%Y-%m-%d", "%m/%d/%Y", "%d/%m/%Y"]:
                    try:
                        parsed_due_date = datetime.strptime(due_date, date_format).isoformat()
                        break
                    except ValueError:
                        continue
                
                if not parsed_due_date:
                    print("Error: Due date must be in YYYY-MM-DD, MM/DD/YYYY, or DD/MM/YYYY format.")
                    return False
                    
            except ValueError:
                print("Error: Invalid due date format.")
                return False
        
        task = {
            "id": len(self.tasks) + 1,
            "description": description.strip(),
            "priority": priority.lower(),
            "completed": False,
            "created_at": datetime.now().isoformat(),
            "due_date": parsed_due_date
        }
        
        self.tasks.append(task)
        if self.save_tasks():
            print(f"✅ Added task: {description}")
            if due_date:
                print(f"   Due: {due_date}")
            return True
        return False
    
    def list_tasks(self, show_completed: bool = False) -> None:
        """List all tasks with due date information."""
        if not self.tasks:
            print("📝 No tasks found. Add some tasks to get started!")
            return
        
        filtered_tasks = [task for task in self.tasks if show_completed or not task["completed"]]
        
        if not filtered_tasks:
            print("🎉 All tasks are completed!")
            return
        
        print(f"\n📋 Your Tasks ({len(filtered_tasks)} total):")
        print("-" * 60)
        
        for task in filtered_tasks:
            status = "✅" if task["completed"] else "⏳"
            priority_emoji = {"high": "🔴", "medium": "🟡", "low": "🟢"}.get(task["priority"], "🟡")
            
            # Format due date display
            due_info = ""
            if task.get("due_date"):
                due_date = datetime.fromisoformat(task["due_date"])
                today = datetime.now().date()
                due_date_only = due_date.date()
                
                if due_date_only < today and not task["completed"]:
                    due_info = f" 🔴 OVERDUE ({due_date.strftime('%m/%d/%Y')})"
                elif due_date_only == today:
                    due_info = f" 🟡 DUE TODAY ({due_date.strftime('%m/%d/%Y')})"
                elif (due_date_only - today).days <= 3:
                    due_info = f" 🟠 DUE SOON ({due_date.strftime('%m/%d/%Y')})"
                else:
                    due_info = f" 📅 Due: {due_date.strftime('%m/%d/%Y')}"
            
            print(f"{status} [{task['id']}] {priority_emoji} {task['description']}{due_info}")
            if task["completed"]:
                print(f"   Completed: {task.get('completed_at', 'Unknown')}")

    def complete_task(self, task_id: int) -> bool:
        """Mark a task as completed."""
        for task in self.tasks:
            if task["id"] == task_id:
                if task["completed"]:
                    print(f"Task {task_id} is already completed.")
                    return True
                
                task["completed"] = True
                task["completed_at"] = datetime.now().isoformat()
                
                if self.save_tasks():
                    print(f"✅ Completed task: {task['description']}")
                    return True
                return False
        
        print(f"❌ Task with ID {task_id} not found.")
        return False
    
    def delete_task(self, task_id: int) -> bool:
        """Delete a task."""
        for i, task in enumerate(self.tasks):
            if task["id"] == task_id:
                deleted_task = self.tasks.pop(i)
                if self.save_tasks():
                    print(f"🗑️  Deleted task: {deleted_task['description']}")
                    return True
                return False
        
        print(f"❌ Task with ID {task_id} not found.")
        return False
    
    def get_stats(self) -> Dict[str, int]:
        """Get task statistics."""
        total = len(self.tasks)
        completed = sum(1 for task in self.tasks if task["completed"])
        pending = total - completed
        
        return {
            "total": total,
            "completed": completed,
            "pending": pending
        }


def print_help():
    """Print help information."""
    help_text = """
📋 Task Manager CLI - Help

Commands:
  add <description> [priority] [due_date] - Add a new task
    priority: high/medium/low
    due_date: YYYY-MM-DD, MM/DD/YYYY, or DD/MM/YYYY
  list [--all]                          - List tasks (--all shows completed)
  complete <id>                         - Mark a task as completed
  delete <id>                           - Delete a task
  due                                   - Show overdue and due today tasks
  upcoming                              - Show tasks due in next 7 days
  stats                                 - Show task statistics
  help                                  - Show this help message
  quit/exit                             - Exit the program

Examples:
  add "Buy groceries" high 2024-01-15
  add "Read a book" medium 01/20/2024
  add "Call mom" low
  list
  due
  upcoming
  complete 1
  delete 2
  stats
"""
    print(help_text)


def main():
    """Main program loop."""
    task_manager = TaskManager()
    
    print("🎯 Welcome to Task Manager CLI!")
    print("Type 'help' for commands or 'quit' to exit.\n")
    
    while True:
        try:
            command = input("📝 > ").strip().split()
            
            if not command:
                continue
            
            cmd = command[0].lower()
            
            if cmd in ['quit', 'exit']:
                print("👋 Goodbye!")
                break
            
            elif cmd == 'help':
                print_help()
            
            elif cmd == 'add':
                if len(command) < 2:
                    print("❌ Usage: add <description> [priority] [due_date]")
                    continue
                
                # Parse command arguments
                args = command[1:]
                description = ""
                priority = "medium"
                due_date = None
                
                # Find priority and due date in arguments
                for i, arg in enumerate(args):
                    if arg in ['high', 'medium', 'low']:
                        priority = arg
                    elif '/' in arg or '-' in arg:  # Looks like a date
                        due_date = arg
                    else:
                        description += arg + " "
                
                description = description.strip()
                
                if not description:
                    print("❌ Task description cannot be empty.")
                    continue
                
                task_manager.add_task(description, priority, due_date)
            
            elif cmd == 'list':
                show_all = '--all' in command
                task_manager.list_tasks(show_all)
            
            elif cmd == 'complete':
                if len(command) != 2:
                    print("❌ Usage: complete <id>")
                    continue
                
                try:
                    task_id = int(command[1])
                    task_manager.complete_task(task_id)
                except ValueError:
                    print("❌ Task ID must be a number.")
            
            elif cmd == 'delete':
                if len(command) != 2:
                    print("❌ Usage: delete <id>")
                    continue
                
                try:
                    task_id = int(command[1])
                    task_manager.delete_task(task_id)
                except ValueError:
                    print("❌ Task ID must be a number.")
            
            elif cmd == 'due':
                """Show tasks due today or overdue."""
                today = datetime.now().date()
                due_tasks = []
                
                for task in task_manager.tasks:
                    if not task["completed"] and task.get("due_date"):
                        due_date = datetime.fromisoformat(task["due_date"]).date()
                        if due_date <= today:
                            due_tasks.append(task)
                
                if due_tasks:
                    print(f"\n🚨 Tasks Due Today or Overdue ({len(due_tasks)} total):")
                    print("-" * 50)
                    for task in due_tasks:
                        due_date = datetime.fromisoformat(task["due_date"]).date()
                        days_overdue = (today - due_date).days
                        if days_overdue > 0:
                            print(f"🔴 [{task['id']}] {task['description']} (OVERDUE by {days_overdue} days)")
                        else:
                            print(f"🟡 [{task['id']}] {task['description']} (DUE TODAY)")
                else:
                    print("🎉 No tasks due today or overdue!")
            
            elif cmd == 'upcoming':
                """Show tasks due in the next 7 days."""
                today = datetime.now().date()
                week_from_now = today + timedelta(days=7)
                upcoming_tasks = []
                
                for task in task_manager.tasks:
                    if not task["completed"] and task.get("due_date"):
                        due_date = datetime.fromisoformat(task["due_date"]).date()
                        if today < due_date <= week_from_now:
                            upcoming_tasks.append(task)
                
                if upcoming_tasks:
                    print(f"\n📅 Tasks Due in Next 7 Days ({len(upcoming_tasks)} total):")
                    print("-" * 50)
                    for task in upcoming_tasks:
                        due_date = datetime.fromisoformat(task["due_date"]).date()
                        days_until = (due_date - today).days
                        print(f"📅 [{task['id']}] {task['description']} (Due in {days_until} days)")
                else:
                    print("📅 No tasks due in the next 7 days!")
            
            elif cmd == 'stats':
                stats = task_manager.get_stats()
                print(f"\n📊 Task Statistics:")
                print(f"   Total tasks: {stats['total']}")
                print(f"   Completed: {stats['completed']}")
                print(f"   Pending: {stats['pending']}")
                
                if stats['total'] > 0:
                    completion_rate = (stats['completed'] / stats['total']) * 100
                    print(f"   Completion rate: {completion_rate:.1f}%")
            
            else:
                print(f"❌ Unknown command: {cmd}")
                print("Type 'help' for available commands.")
        
        except KeyboardInterrupt:
            print("\n👋 Goodbye!")
            break
        except Exception as e:
            print(f"❌ An error occurred: {e}")


if __name__ == "__main__":
    main()