package de.tklein.tklab.openproject.mcp.security;

public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }
}
