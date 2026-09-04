import os
from langchain_openai import ChatOpenAI
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain.tools import tool
from langchain_core.prompts import ChatPromptTemplate

# 1. Create a simple tool called add_numbers
@tool
def add_numbers(a: int, b: int) -> int:
    """Adds two integers together and returns the sum. Use this tool when you need to add numbers."""
    return a + b

def main():
    # Check if API key is set
    if not os.environ.get("OPENAI_API_KEY"):
        print("Error: Please set your OPENAI_API_KEY environment variable before running.")
        print("You can set it in your terminal or use a .env file.")
        return

    # 2. Connect to the LLM (Language Model)
    # temperature=0 makes the AI deterministic (it will give the same answer every time)
    llm = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)

    # 3. Create a simple prompt telling the agent what to do
    prompt = ChatPromptTemplate.from_messages([
        ("system", "You are a helpful mathematical assistant. Use the provided tools to answer questions."),
        ("human", "{input}"),
        ("placeholder", "{agent_scratchpad}"),
    ])

    # 4. Create the agent that can decide when to use the tool
    tools = [add_numbers]
    agent = create_tool_calling_agent(llm, tools, prompt)
    
    # 5. Create an executor to run the agent (verbose=True lets us see its thought process)
    agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

    # 6. Give the agent a user query
    query = "What is 25 + 35?"
    print(f"--- Asking the agent: '{query}' ---\n")
    
    # 7. The agent uses the tool and returns the final answer
    response = agent_executor.invoke({"input": query})
    print(f"\n--- Final Answer: {response['output']} ---")

if __name__ == "__main__":
    main()
