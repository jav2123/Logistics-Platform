# Etapa 1: Construcción - Actualizado a SDK 9.0
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copiamos el archivo de proyecto y restauramos dependencias
COPY *.csproj ./
RUN dotnet restore

# Copiamos el resto del código y publicamos
COPY . .
RUN dotnet publish -c Release -o /app/publish

# Etapa 2: Imagen de ejecución (Runtime) - Actualizado a ASP.NET 9.0
FROM mcr.microsoft.com/dotnet/aspnet:9.0
WORKDIR /app
COPY --from=build /app/publish .

# Puerto de escucha para .NET 9 (Nota: En .NET 8/9 el puerto por defecto interno de este contenedor suele ser el 8080)
EXPOSE 8080
ENV ASPNETCORE_URLS=http://+:8080

ENTRYPOINT ["dotnet", "src.dll"]